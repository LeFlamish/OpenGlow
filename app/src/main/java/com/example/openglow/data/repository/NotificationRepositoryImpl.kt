package com.example.openglow.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.example.openglow.NotificationTargetPackages
import com.example.openglow.ReceivedNotification
import com.example.openglow.data.entity.AnalysisLogEntity
import com.example.openglow.data.entity.IdentifierType
import com.example.openglow.data.entity.NoteEntity
import com.example.openglow.data.entity.NotificationEntity
import com.example.openglow.data.entity.SenderEntity
import com.example.openglow.data.entity.SenderIdentifierEntity
import com.example.openglow.data.entity.SenderType
import com.example.openglow.data.entity.SummaryEntity
import com.example.openglow.data.local.AppDatabase
import com.example.openglow.data.local.dao.AnalysisLogDao
import com.example.openglow.data.local.dao.NoteDao
import com.example.openglow.data.local.dao.NotificationDao
import com.example.openglow.data.local.dao.SenderDao
import com.example.openglow.data.local.dao.SummaryDao
import com.example.openglow.domain.llm.NoteUpdateInput
import com.example.openglow.domain.llm.NotificationAnalysisResult
import com.example.openglow.domain.llm.SenderScope
import com.example.openglow.domain.llm.LlmRouter
import com.example.openglow.domain.personalization.PersonalizationEngine
import java.security.MessageDigest
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray

class NotificationRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val senderDao: SenderDao,
    private val notificationDao: NotificationDao,
    private val summaryDao: SummaryDao,
    private val noteDao: NoteDao,
    private val analysisLogDao: AnalysisLogDao,
    private val llmRouter: LlmRouter,
    private val personalizationEngine: PersonalizationEngine,
) : NotificationRepository {

    private val insertMutex = Mutex()
    private val analysisMutex = Mutex()

    override suspend fun handleNewNotification(
        notification: ReceivedNotification,
    ) = withContext(Dispatchers.IO) {
        if (notification.packageName !in NotificationTargetPackages.targetPackages) {
            Log.w(TAG, "Skipping non-target package defensively: ${notification.packageName}")
            return@withContext
        }

        val fullText = notification.extractedFullText
            .ifBlank { notification.bigText.orEmpty() }
            .ifBlank { notification.text.orEmpty() }
            .trim()

        if (fullText.isBlank()) {
            Log.w(TAG, "Skipping blank extracted notification: package=${notification.packageName}")
            return@withContext
        }

        val platform = platformFromPackage(notification.packageName)
        val senderScope = detectSenderScope(notification)
        val senderDisplayName = resolveSenderDisplayName(notification, senderScope)
        val senderName = notification.title
        val identifierType = IdentifierType.DISPLAY_NAME
        val identifierValue = "$platform|${senderScope.name}|$senderDisplayName"
        val textFragmentsJson = JSONArray(notification.textFragments).toString()
        val content = fullText.limitForStorage()

        val stored = insertMutex.withLock {
            db.withTransaction {
                var identifier = senderDao.findIdentifier(identifierValue, platform, identifierType)
                val senderType = senderScope.toSenderType()

                if (identifier == null) {
                    val senderId = senderDao.insertSender(
                        SenderEntity(
                            displayName = senderDisplayName,
                            type = senderType,
                            lastNotifiedAt = notification.postTime,
                        ),
                    )
                    val identifierId = senderDao.insertIdentifier(
                        SenderIdentifierEntity(
                            senderId = senderId,
                            platform = platform,
                            identifierValue = identifierValue,
                            identifierType = identifierType,
                        ),
                    )
                    identifier = SenderIdentifierEntity(
                        id = identifierId,
                        senderId = senderId,
                        platform = platform,
                        identifierValue = identifierValue,
                        identifierType = identifierType,
                    )
                } else {
                    senderDao.updateLastNotifiedAt(identifier.senderId, notification.postTime)
                    senderDao.getSenderById(identifier.senderId)?.let { existing ->
                        if (existing.type != senderType || existing.displayName != senderDisplayName) {
                            senderDao.updateSender(
                                existing.copy(
                                    displayName = senderDisplayName,
                                    type = senderType,
                                    lastNotifiedAt = notification.postTime,
                                ),
                            )
                        }
                    }
                }

                val resolvedIdentifier = checkNotNull(identifier)
                val notificationId = notificationDao.insertNotification(
                    NotificationEntity(
                        identifierId = resolvedIdentifier.id,
                        senderName = senderName,
                        packageName = notification.packageName,
                        content = content,
                        textFragmentsJson = textFragmentsJson,
                        completenessConfidence = notification.completenessConfidence,
                        isLikelyComplete = notification.isLikelyComplete,
                        timestamp = notification.postTime,
                    ),
                )

                StoredNotification(
                    notificationId = notificationId,
                    senderId = resolvedIdentifier.senderId,
                    senderDisplayName = senderDisplayName,
                    senderScope = senderScope,
                )
            }
        }

        Log.i(
            TAG,
            "Notification saved: senderId=${stored.senderId}, scope=${stored.senderScope}, " +
                "platform=$platform, textLength=${content.length}, confidence=${notification.completenessConfidence}",
        )

        analyzeAndPersist(
            notificationId = stored.notificationId,
            senderId = stored.senderId,
            senderDisplayName = stored.senderDisplayName,
            senderScope = stored.senderScope,
            platform = platform,
            packageName = notification.packageName,
            senderName = senderName,
            fullText = content,
            textFragments = notification.textFragments,
            completenessConfidence = notification.completenessConfidence,
            timestamp = notification.postTime,
        )
    }

    override suspend fun generateSummaryIfNeeded(senderId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val sender = senderDao.getSenderById(senderId)
                ?: return@runCatching
            val unsummarized = notificationDao.getUnsummarizedNotificationsBySender(senderId)
            val latest = unsummarized.lastOrNull()
                ?: return@runCatching

            analyzeAndPersist(
                notificationId = latest.id,
                senderId = senderId,
                senderDisplayName = sender.displayName,
                senderScope = sender.type.toSenderScope(),
                platform = platformFromPackage(latest.packageName),
                packageName = latest.packageName,
                senderName = latest.senderName,
                fullText = unsummarized.joinToString(separator = "\n") { it.content },
                textFragments = unsummarized.flatMap { parseJsonArray(it.textFragmentsJson) },
                completenessConfidence = unsummarized.minOfOrNull { it.completenessConfidence } ?: 0.5f,
                timestamp = latest.timestamp,
                notificationIdsToMark = unsummarized.map { it.id },
            )
        }
    }

    private suspend fun analyzeAndPersist(
        notificationId: Long,
        senderId: Long,
        senderDisplayName: String,
        senderScope: SenderScope,
        platform: String,
        packageName: String,
        senderName: String?,
        fullText: String,
        textFragments: List<String>,
        completenessConfidence: Float,
        timestamp: Long,
        notificationIdsToMark: List<Long> = listOf(notificationId),
    ) {
        analysisMutex.withLock {
            val previousNote = noteDao.getNoteBySenderId(senderId)
            val personalizationRules = personalizationEngine.loadRules()
            val input = NoteUpdateInput(
                platform = platform,
                packageName = packageName,
                senderName = senderName,
                senderDisplayName = senderDisplayName,
                senderScope = senderScope,
                previousFinalSummary = previousNote?.finalSummary,
                previousRetainedFacts = previousNote?.retainedFactsJson?.let(::parseJsonArray).orEmpty(),
                newNotificationText = fullText,
                textFragments = textFragments,
                completenessConfidence = completenessConfidence,
                timestamp = timestamp,
                userPersonalizationRules = personalizationRules,
            )

            val analysis = llmRouter.analyzeAndUpdateNote(input)
            val now = System.currentTimeMillis()

            db.withTransaction {
                val noteId = upsertNote(
                    previous = noteDao.getNoteBySenderId(senderId),
                    senderId = senderId,
                    senderDisplayName = senderDisplayName,
                    platform = platform,
                    analysis = analysis,
                    now = now,
                )

                summaryDao.insertSummary(
                    SummaryEntity(
                        senderId = senderId,
                        summarizedText = analysis.oneLineSummary,
                        importance = analysis.importance.name,
                        isWorkRelated = analysis.isWorkRelated,
                        noteTitle = analysis.noteTitle,
                        actionItemsJson = JSONArray(analysis.actionItems).toString(),
                        deadlineText = analysis.deadlineText,
                        createdAt = now,
                        notificationCount = 1,
                    ),
                )

                analysisLogDao.insertAnalysisLog(
                    AnalysisLogEntity(
                        notificationId = notificationId,
                        noteId = noteId,
                        senderId = senderId,
                        inputTextPreview = fullText.take(MAX_INPUT_PREVIEW_LENGTH),
                        inputTextHash = fullText.sha256(),
                        oneLineSummary = analysis.oneLineSummary,
                        importance = analysis.importance.name,
                        isWorkRelated = analysis.isWorkRelated,
                        meetingDetected = analysis.meetingDetected,
                        projectDetected = analysis.projectDetected,
                        senderScope = analysis.senderScope.name,
                        modelSource = analysis.modelSource.name,
                        confidence = analysis.confidence,
                        createdAt = now,
                        feedbackRequested = analysis.shouldAskFeedback,
                    ),
                )

                notificationDao.markAsSummarized(notificationIdsToMark)
            }

            Log.i(
                TAG,
                "Analysis saved: senderId=$senderId, model=${analysis.modelSource}, " +
                    "importance=${analysis.importance}, work=${analysis.isWorkRelated}",
            )
        }
    }

    private suspend fun upsertNote(
        previous: NoteEntity?,
        senderId: Long,
        senderDisplayName: String,
        platform: String,
        analysis: NotificationAnalysisResult,
        now: Long,
    ): Long {
        val retainedFactsJson = JSONArray(analysis.retainedFacts).toString()
        val actionItemsJson = JSONArray(analysis.actionItems).toString()
        val existing = previous

        if (existing == null) {
            return noteDao.insertNote(
                NoteEntity(
                    senderId = senderId,
                    platform = platform,
                    senderDisplayName = senderDisplayName,
                    senderScope = analysis.senderScope.name,
                    title = analysis.noteTitle.ifBlank { senderDisplayName },
                    finalSummary = analysis.updatedFinalSummary,
                    retainedFactsJson = retainedFactsJson,
                    latestOneLineSummary = analysis.oneLineSummary,
                    latestImportance = analysis.importance.name,
                    latestIsWorkRelated = analysis.isWorkRelated,
                    latestMeetingDetected = analysis.meetingDetected,
                    latestProjectDetected = analysis.projectDetected,
                    latestActionItemsJson = actionItemsJson,
                    latestDeadlineText = analysis.deadlineText,
                    notificationCount = 1,
                    modelSource = analysis.modelSource.name,
                    confidence = analysis.confidence,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }

        noteDao.updateNote(
            existing.copy(
                platform = platform,
                senderDisplayName = senderDisplayName,
                senderScope = analysis.senderScope.name,
                title = analysis.noteTitle.ifBlank { existing.title },
                finalSummary = analysis.updatedFinalSummary,
                retainedFactsJson = retainedFactsJson,
                latestOneLineSummary = analysis.oneLineSummary,
                latestImportance = analysis.importance.name,
                latestIsWorkRelated = analysis.isWorkRelated,
                latestMeetingDetected = analysis.meetingDetected,
                latestProjectDetected = analysis.projectDetected,
                latestActionItemsJson = actionItemsJson,
                latestDeadlineText = analysis.deadlineText,
                notificationCount = existing.notificationCount + 1,
                modelSource = analysis.modelSource.name,
                confidence = analysis.confidence,
                updatedAt = now,
            ),
        )
        return existing.id
    }

    override fun getAllSendersStream(): Flow<List<SenderEntity>> = senderDao.getAllSendersFlow()

    override fun getSummariesStream(senderId: Long): Flow<List<SummaryEntity>> {
        return summaryDao.getSummariesBySenderFlow(senderId)
    }

    override fun getNotificationsStream(senderId: Long): Flow<List<NotificationEntity>> {
        return notificationDao.getNotificationsBySenderFlow(senderId)
    }

    override fun getAllNotesStream(): Flow<List<NoteEntity>> = noteDao.getAllNotesFlow()

    override fun getNotesByScopeStream(scope: String): Flow<List<NoteEntity>> {
        return noteDao.getNotesByScopeFlow(scope)
    }

    override suspend fun getNoteBySenderId(senderId: Long): NoteEntity? = withContext(Dispatchers.IO) {
        noteDao.getNoteBySenderId(senderId)
    }

    override suspend fun mergeSenders(oldSenderId: Long, newSenderId: Long) {
        withContext(Dispatchers.IO) {
            runCatching {
                db.withTransaction {
                    senderDao.mergeIdentifiersToNewSender(oldSenderId, newSenderId)
                    summaryDao.mergeSummariesToNewSender(oldSenderId, newSenderId)
                    noteDao.mergeNotesToNewSender(oldSenderId, newSenderId)
                    senderDao.deleteSenderById(oldSenderId)
                }
            }.onFailure {
                Log.e(TAG, "Failed to merge senders: ${it.message}")
            }
        }
    }

    private fun detectSenderScope(notification: ReceivedNotification): SenderScope {
        if (notification.groupHint == true) return SenderScope.GROUP

        val conversationTitle = notification.conversationTitle?.trim()
        val title = notification.title?.trim()
        if (!conversationTitle.isNullOrBlank() && !conversationTitle.equals(title, ignoreCase = true)) {
            return SenderScope.GROUP
        }

        val messageSenders = notification.textFragments
            .mapNotNull { fragment -> fragment.substringBefore(":", missingDelimiterValue = "").trim().takeIf { it.isNotBlank() } }
            .distinct()
        if (messageSenders.size > 1) return SenderScope.GROUP

        if (notification.packageName == "com.kakao.talk" && !notification.subText.isNullOrBlank()) {
            return SenderScope.GROUP
        }

        if (isEmailOrSms(notification.packageName)) {
            val threadText = listOfNotNull(notification.title, notification.subText, notification.conversationTitle)
                .joinToString(" ")
            if (GROUP_HINT_KEYWORDS.any { threadText.contains(it, ignoreCase = true) }) {
                return SenderScope.GROUP
            }
            return SenderScope.INDIVIDUAL
        }

        return if (notification.groupHint == false) SenderScope.INDIVIDUAL else SenderScope.UNKNOWN
    }

    private fun resolveSenderDisplayName(
        notification: ReceivedNotification,
        senderScope: SenderScope,
    ): String {
        return when (senderScope) {
            SenderScope.GROUP -> notification.conversationTitle
                ?: notification.subText
                ?: notification.title
                ?: notification.appName
            SenderScope.INDIVIDUAL -> notification.title
                ?: notification.conversationTitle
                ?: notification.subText
                ?: notification.appName
            SenderScope.UNKNOWN -> notification.conversationTitle
                ?: notification.title
                ?: notification.subText
                ?: notification.appName
        }.ifBlank { notification.packageName }
    }

    private fun platformFromPackage(packageName: String): String = when (packageName) {
        "com.kakao.talk" -> "KAKAO"
        "com.samsung.android.messaging",
        "com.google.android.apps.messaging" -> "SMS"
        "com.google.android.gm",
        "com.samsung.android.email.provider",
        "com.microsoft.office.outlook" -> "EMAIL"
        else -> "OTHER"
    }

    private fun isEmailOrSms(packageName: String): Boolean {
        return packageName in setOf(
            "com.samsung.android.messaging",
            "com.google.android.apps.messaging",
            "com.google.android.gm",
            "com.samsung.android.email.provider",
            "com.microsoft.office.outlook",
        )
    }

    private fun SenderScope.toSenderType(): SenderType = when (this) {
        SenderScope.INDIVIDUAL -> SenderType.INDIVIDUAL
        SenderScope.GROUP -> SenderType.GROUP
        SenderScope.UNKNOWN -> SenderType.UNKNOWN
    }

    private fun SenderType.toSenderScope(): SenderScope = when (this) {
        SenderType.INDIVIDUAL -> SenderScope.INDIVIDUAL
        SenderType.GROUP -> SenderScope.GROUP
        SenderType.UNKNOWN -> SenderScope.UNKNOWN
    }

    private fun String.limitForStorage(): String {
        return if (length > MAX_NOTIFICATION_LENGTH) take(MAX_NOTIFICATION_LENGTH) else this
    }

    private fun parseJsonArray(json: String): List<String> {
        return runCatching {
            val array = JSONArray(json)
            List(array.length()) { index -> array.optString(index) }
                .filter { it.isNotBlank() }
        }.getOrDefault(emptyList())
    }

    private fun String.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(toByteArray(Charsets.UTF_8))
        return digest.joinToString(separator = "") { "%02x".format(it) }
    }

    private data class StoredNotification(
        val notificationId: Long,
        val senderId: Long,
        val senderDisplayName: String,
        val senderScope: SenderScope,
    )

    private companion object {
        private const val TAG = "NotificationRepo"
        private const val MAX_NOTIFICATION_LENGTH = 4000
        private const val MAX_INPUT_PREVIEW_LENGTH = 120
        private val GROUP_HINT_KEYWORDS = listOf("team", "group", "팀", "단체", "프로젝트", "수신자")
    }
}
