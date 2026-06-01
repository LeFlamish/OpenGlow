package com.example.openglow.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.openglow.data.entity.FeedbackEntity
import com.example.openglow.data.entity.NoteEntity
import com.example.openglow.data.local.dao.AnalysisLogDao
import com.example.openglow.data.local.dao.FeedbackDao
import com.example.openglow.data.local.dao.NoteDao
import com.example.openglow.data.repository.NotificationRepository
import com.example.openglow.domain.personalization.PersonalizationEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray

enum class NoteFilter(val label: String) {
    ALL("전체"),
    INDIVIDUAL("개인"),
    GROUP("그룹"),
}

enum class ImportanceFilter(val label: String) {
    ALL("전체"),
    LOW("낮음"),
    NORMAL("보통"),
    HIGH("높음"),
    URGENT("긴급"),
}

data class NoteUiState(
    val selectedFilter: NoteFilter = NoteFilter.ALL,
    val selectedImportance: ImportanceFilter = ImportanceFilter.ALL,
    val query: String = "",
    val workOnly: Boolean = false,
    val notes: List<NoteUiModel> = emptyList(),
    val feedbackMessage: String? = null,
)

private data class NoteControls(
    val selectedFilter: NoteFilter,
    val selectedImportance: ImportanceFilter,
    val query: String,
    val workOnly: Boolean,
    val feedbackMessage: String?,
)

data class NoteUiModel(
    val id: Long,
    val title: String,
    val senderDisplayName: String,
    val senderScope: String,
    val platform: String,
    val finalSummary: String,
    val retainedFacts: List<String>,
    val latestOneLineSummary: String,
    val latestImportance: String,
    val latestIsWorkRelated: Boolean,
    val latestMeetingDetected: Boolean,
    val latestProjectDetected: Boolean,
    val actionItems: List<String>,
    val latestDeadlineText: String?,
    val notificationCount: Int,
    val modelSource: String,
    val confidence: Float,
    val updatedAtText: String,
)

@HiltViewModel
class NoteViewModel @Inject constructor(
    repository: NotificationRepository,
    private val analysisLogDao: AnalysisLogDao,
    private val feedbackDao: FeedbackDao,
    private val noteDao: NoteDao,
    private val personalizationEngine: PersonalizationEngine,
) : ViewModel() {

    private val selectedFilter = MutableStateFlow(NoteFilter.ALL)
    private val selectedImportance = MutableStateFlow(ImportanceFilter.ALL)
    private val query = MutableStateFlow("")
    private val workOnly = MutableStateFlow(false)
    private val feedbackMessage = MutableStateFlow<String?>(null)

    private val controls = combine(
        selectedFilter,
        selectedImportance,
        query,
        workOnly,
        feedbackMessage,
    ) { filter, importance, queryText, onlyWork, message ->
        NoteControls(filter, importance, queryText, onlyWork, message)
    }

    val uiState: StateFlow<NoteUiState> = combine(
        repository.getAllNotesStream(),
        controls,
    ) { notes, controls ->
        val filter = controls.selectedFilter
        val importance = controls.selectedImportance
        val queryText = controls.query
        val onlyWork = controls.workOnly
        val normalizedQuery = queryText.trim().lowercase(Locale.KOREA)
        val filtered = notes
            .filter { note ->
                when (filter) {
                    NoteFilter.ALL -> true
                    NoteFilter.INDIVIDUAL -> note.senderScope == "INDIVIDUAL" || note.senderScope == "UNKNOWN"
                    NoteFilter.GROUP -> note.senderScope == "GROUP"
                }
            }
            .filter { note ->
                importance == ImportanceFilter.ALL || note.latestImportance == importance.name
            }
            .filter { note -> !onlyWork || note.latestIsWorkRelated }
            .filter { note ->
                normalizedQuery.isBlank() ||
                    note.title.lowercase(Locale.KOREA).contains(normalizedQuery) ||
                    note.senderDisplayName.lowercase(Locale.KOREA).contains(normalizedQuery) ||
                    note.finalSummary.lowercase(Locale.KOREA).contains(normalizedQuery)
            }
            .map(::toUiModel)

        NoteUiState(
            selectedFilter = filter,
            selectedImportance = importance,
            query = queryText,
            workOnly = onlyWork,
            notes = filtered,
            feedbackMessage = controls.feedbackMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NoteUiState(),
    )

    fun selectFilter(filter: NoteFilter) {
        selectedFilter.value = filter
    }

    fun selectImportance(filter: ImportanceFilter) {
        selectedImportance.value = filter
    }

    fun updateQuery(value: String) {
        query.value = value
    }

    fun setWorkOnly(value: Boolean) {
        workOnly.value = value
    }

    fun clearFeedbackMessage() {
        feedbackMessage.value = null
    }

    fun submitFeedback(
        noteId: Long,
        correctedImportance: String?,
        correctedIsWorkRelated: Boolean?,
        correctedSenderScope: String?,
        userComment: String?,
    ) {
        viewModelScope.launch {
            runCatching {
                val note = noteDao.getNoteById(noteId)
                val log = analysisLogDao.getLatestAnalysisLogByNoteId(noteId)
                    ?: error("No analysis log for note")
                val feedback = FeedbackEntity(
                    analysisLogId = log.id,
                    noteId = noteId,
                    originalImportance = log.importance,
                    correctedImportance = correctedImportance?.takeIf { it != log.importance },
                    originalIsWorkRelated = log.isWorkRelated,
                    correctedIsWorkRelated = correctedIsWorkRelated?.takeIf { it != log.isWorkRelated },
                    originalSenderScope = log.senderScope,
                    correctedSenderScope = correctedSenderScope?.takeIf { it != log.senderScope },
                    userComment = userComment?.takeIf { it.isNotBlank() },
                    createdAt = System.currentTimeMillis(),
                )
                val feedbackId = feedbackDao.insertFeedback(feedback)
                val savedFeedback = feedback.copy(id = feedbackId)
                personalizationEngine.recordFeedback(savedFeedback, log, note)
                analysisLogDao.markFeedbackResolved(log.id)
            }.onSuccess {
                feedbackMessage.value = "피드백을 저장하고 개인화 규칙에 반영했어요."
            }.onFailure {
                Log.e(TAG, "Failed to save feedback: ${it.message}")
                feedbackMessage.value = "피드백 저장에 실패했어요."
            }
        }
    }

    private fun toUiModel(note: NoteEntity): NoteUiModel {
        return NoteUiModel(
            id = note.id,
            title = note.title,
            senderDisplayName = note.senderDisplayName,
            senderScope = note.senderScope,
            platform = note.platform,
            finalSummary = note.finalSummary,
            retainedFacts = parseJsonArray(note.retainedFactsJson),
            latestOneLineSummary = note.latestOneLineSummary,
            latestImportance = note.latestImportance,
            latestIsWorkRelated = note.latestIsWorkRelated,
            latestMeetingDetected = note.latestMeetingDetected,
            latestProjectDetected = note.latestProjectDetected,
            actionItems = parseJsonArray(note.latestActionItemsJson),
            latestDeadlineText = note.latestDeadlineText,
            notificationCount = note.notificationCount,
            modelSource = note.modelSource,
            confidence = note.confidence,
            updatedAtText = DATE_FORMAT.format(Date(note.updatedAt)),
        )
    }

    private fun parseJsonArray(json: String): List<String> {
        return runCatching {
            val array = JSONArray(json)
            List(array.length()) { index -> array.optString(index) }
                .filter { it.isNotBlank() }
        }.getOrElse {
            Log.w(TAG, "Failed to parse note JSON array: ${it.message}")
            emptyList()
        }
    }

    private companion object {
        private const val TAG = "NoteViewModel"
        private val DATE_FORMAT = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA)
    }
}
