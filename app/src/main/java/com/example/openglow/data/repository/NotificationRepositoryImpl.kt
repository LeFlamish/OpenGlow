package com.example.openglow.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.example.openglow.ReceivedNotification
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import com.example.openglow.data.local.dao.*
import com.example.openglow.data.entity.*
import com.example.openglow.data.local.AppDatabase
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class NotificationRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val senderDao: SenderDao,
    private val notificationDao: NotificationDao,
    private val summaryDao: SummaryDao
    // private val llmApiService: LlmApiService // TODO: LLM API 확정 시 주석 해제
) : NotificationRepository {

    private val insertMutex = Mutex()

    // =======================================================
    // 1. 알림 수신 및 파싱 로직
    // =======================================================
    override suspend fun handleNewNotification(
        notification: ReceivedNotification
    ) = withContext(Dispatchers.IO) {

        if (notification.title == null && notification.text == null && notification.bigText == null) {
            return@withContext // 파싱하지 않고 즉시 종료 (무시)
        }

        // 1-1. 플랫폼 구분 (패키지명 기준)
        val platform = when (notification.packageName) {
            "com.kakao.talk" -> "KAKAO"
            "com.samsung.android.messaging",
            "com.google.android.apps.messaging" -> "SMS"
            "com.google.android.gm",
            "com.samsung.android.email.provider",
            "com.microsoft.office.outlook" -> "EMAIL"
            else -> "OTHER" // Service에서 이미 걸렀으므로 도달할 확률은 매우 낮음
        }

        // 1-2. 단톡방 여부 확인 (extras 활용)
        val isGroup = notification.extras["android.isGroupConversation"] as? Boolean ?: false

        // 1-3. 식별자(방 이름)와 실제 발신자(이름) 추출
        val identifierValue = if (isGroup) {
            notification.conversationTitle ?: notification.subText ?: notification.title ?: "알 수 없는 방"
        } else {
            notification.title ?: "알 수 없는 발신자"
        }

        val senderName = notification.title

        // 본문 추출 및 길이 제한 (CursorWindow 및 LLM 토큰 초과 방어)
        val rawContent = notification.bigText ?: notification.text ?: ""
        if (rawContent.isBlank()) return@withContext

        val content = if (rawContent.length > 3000) { // 최대 길이는 임시로 정함
            rawContent.take(3000) + "...\n(내용이 너무 길어 생략됨)"
        } else {
            rawContent
        }

        // 이 예제에서는 알림에서 추출한 이름(문자열)을 식별값으로 사용하므로 DISPLAY_NAME 타입으로 고정
        val identifierType = IdentifierType.DISPLAY_NAME

        try {
            insertMutex.withLock {
                db.withTransaction {
                    // 2. DB에서 기존 식별자 검색
                    var identifier = senderDao.findIdentifier(identifierValue, platform, identifierType)

                    if (identifier == null) {
                        // 처음 보는 방/발신자인 경우: 새로 생성
                        val newSenderId = senderDao.insertSender(
                            SenderEntity(
                                displayName = identifierValue,
                                type = if (isGroup) SenderType.GROUP else SenderType.INDIVIDUAL,
                                lastNotifiedAt = notification.postTime
                            )
                        )
                        val newIdentifier = SenderIdentifierEntity(
                            senderId = newSenderId,
                            platform = platform,
                            identifierValue = identifierValue,
                            identifierType = identifierType
                        )
                        val newIdentifierId = senderDao.insertIdentifier(newIdentifier)
                        identifier = newIdentifier.copy(id = newIdentifierId)
                    } else {
                        // 아는 방/발신자인 경우: 최근 알림 시간만 업데이트
                        senderDao.updateLastNotifiedAt(identifier.senderId, notification.postTime)
                    }

                    // 3. 알림 원본 DB 저장
                    notificationDao.insertNotification(
                        NotificationEntity(
                            identifierId = identifier.id,
                            senderName = senderName,
                            packageName = notification.packageName,
                            content = content,
                            timestamp = notification.postTime
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Failed to save notification: ${e.message}")
        }
    }

    // =======================================================
    // 2. 요약 생성 로직 (현재 LLM API 미확정으로 로직 뼈대만 유지)
    // =======================================================
    override suspend fun generateSummaryIfNeeded(senderId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. DB에서 요약 안 된 알림들 가져오기
            val unsummarized = notificationDao.getUnsummarizedNotificationsBySender(senderId)

            // 알림이 일정 개수 이하면 스킵
            if (unsummarized.size < 3) return@withContext Result.success(Unit)

            // 2. LLM에게 보낼 프롬프트 텍스트 가공
            val conversationText = unsummarized.joinToString(separator = "\n") {
                "${it.senderName ?: "알 수 없음"}: ${it.content}"
            }

            /* TODO: LLM API 호출 방식 확정 시 아래 주석 해제 및 적용

            // 3. 외부 LLM API 호출
            val response = llmApiService.requestSummary(
                SummaryRequestDto(prompt = "다음 대화를 핵심만 요약해줘:\n$conversationText")
            )
            val generatedText = response.summaryText

            db.withTransaction {
                // 4. 요약 결과 DB에 저장
                summaryDao.insertSummary(
                    SummaryEntity(
                        senderId = senderId,
                        summarizedText = generatedText,
                        createdAt = System.currentTimeMillis(),
                        notificationCount = unsummarized.size
                    )
                )

                // 5. 사용된 알림들을 '요약 완료' 상태로 변경
                val notificationIds = unsummarized.map { it.id }
                notificationDao.markAsSummarized(notificationIds)
            }
            */

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Summary generation failed: ${e.message}")
            Result.failure(e)
        }
    }

    // =======================================================
    // 3. UI 데이터 제공 로직 (Flow)
    // =======================================================
    override fun getAllSendersStream(): Flow<List<SenderEntity>> {
        return senderDao.getAllSendersFlow()
    }

    override fun getSummariesStream(senderId: Long): Flow<List<SummaryEntity>> {
        return summaryDao.getSummariesBySenderFlow(senderId)
    }

    override fun getNotificationsStream(senderId: Long): Flow<List<NotificationEntity>> {
        return notificationDao.getNotificationsBySenderFlow(senderId)
    }

    override suspend fun mergeSenders(oldSenderId: Long, newSenderId: Long) {
        withContext(Dispatchers.IO) {
            try {
                db.withTransaction {
                    // 1. 식별자들의 소속을 새 발신자로 모두 이동
                    senderDao.mergeIdentifiersToNewSender(oldSenderId, newSenderId)
                    // 2. 기존 요약 데이터들도 새 발신자로 이동
                    summaryDao.mergeSummariesToNewSender(oldSenderId, newSenderId)
                    // 3. 옛날 발신자 데이터는 삭제
                    senderDao.deleteSenderById(oldSenderId)
                }
            } catch (e: Exception) {
                Log.e("NotificationRepo", "Failed to merge senders: ${e.message}")
            }
        }
    }
}