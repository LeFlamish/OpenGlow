package com.example.openglow.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.openglow.data.entity.SenderEntity
import com.example.openglow.data.entity.SummaryEntity
import com.example.openglow.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class MainDashboardUiState(
    val sender: SenderEntity? = null,
    val latestSummary: SummaryEntity? = null,
)

@HiltViewModel
class MainDashboardViewModel @Inject constructor(
    private val repository: NotificationRepository,
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<MainDashboardUiState> = repository.getAllSendersStream()
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
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MainDashboardUiState(),
        )
}
