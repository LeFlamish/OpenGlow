package com.example.openglow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.openglow.data.entity.NotificationEntity
import com.example.openglow.data.entity.SenderEntity
import com.example.openglow.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TestViewModel @Inject constructor(
    private val repository: NotificationRepository
) : ViewModel() {

    // DB에 있는 모든 발신자 목록을 실시간으로 관찰
    val sendersList: StateFlow<List<SenderEntity>> = repository.getAllSendersStream()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 특정 발신자의 알림 목록을 가져오는 함수
    fun getNotifications(senderId: Long): Flow<List<NotificationEntity>> {
        return repository.getNotificationsStream(senderId)
    }
}