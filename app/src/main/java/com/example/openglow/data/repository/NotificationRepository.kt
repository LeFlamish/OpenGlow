package com.example.openglow.data.repository

import com.example.openglow.ReceivedNotification
import com.example.openglow.data.entity.NotificationEntity
import com.example.openglow.data.entity.SenderEntity
import com.example.openglow.data.entity.SummaryEntity
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {

    // 1. [백그라운드 서비스용] 새 알림 수신 시 처리 및 저장
    suspend fun handleNewNotification(notification: ReceivedNotification)

    // 2. [핵심] 특정 발신자의 알림을 모아서 LLM 요약 요청
    suspend fun generateSummaryIfNeeded(senderId: Long): Result<Unit>

    // 3. [UI 레이어용] 화면에 뿌려줄 데이터 스트림
    fun getAllSendersStream(): Flow<List<SenderEntity>>
    fun getSummariesStream(senderId: Long): Flow<List<SummaryEntity>>
    fun getNotificationsStream(senderId: Long): Flow<List<NotificationEntity>>

    // 4. [UI 레이어용] 발신자 수동 병합
    suspend fun mergeSenders(oldSenderId: Long, newSenderId: Long)
}