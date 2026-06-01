package com.example.openglow.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.openglow.data.entity.CalendarSuggestionEntity
import com.example.openglow.data.entity.SenderEntity
import com.example.openglow.data.entity.SummaryEntity
import com.example.openglow.data.local.dao.CalendarSuggestionDao
import com.example.openglow.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainDashboardUiState(
    val sender: SenderEntity? = null,
    val latestSummary: SummaryEntity? = null,
    val pendingCalendarSuggestion: CalendarSuggestionEntity? = null,
)

@HiltViewModel
class MainDashboardViewModel @Inject constructor(
    private val repository: NotificationRepository,
    private val calendarSuggestionDao: CalendarSuggestionDao,
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val summaryState = repository.getAllSendersStream()
        .flatMapLatest { senders ->
            val latestSender = senders.firstOrNull()
            if (latestSender == null) {
                flowOf(MainDashboardUiState())
            } else {
                repository.getSummariesStream(latestSender.id).map { summaries ->
                    MainDashboardUiState(
                        sender = latestSender,
                        latestSummary = summaries.firstOrNull(),
                    )
                }
            }
        }

    val uiState: StateFlow<MainDashboardUiState> = combine(
        summaryState,
        calendarSuggestionDao.getPendingSuggestionsFlow(),
    ) { state, suggestions ->
        state.copy(pendingCalendarSuggestion = suggestions.firstOrNull())
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MainDashboardUiState(),
        )

    fun resolveCalendarSuggestion(id: Long, approved: Boolean) {
        viewModelScope.launch {
            calendarSuggestionDao.updateStatus(
                id = id,
                status = if (approved) "APPROVED" else "DISMISSED",
                resolvedAt = System.currentTimeMillis(),
            )
        }
    }
}
