package com.example.openglow.data.repository

import com.example.openglow.ReceivedNotification
import com.example.openglow.data.entity.NoteEntity
import com.example.openglow.data.entity.NotificationEntity
import com.example.openglow.data.entity.SenderEntity
import com.example.openglow.data.entity.SummaryEntity
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    suspend fun handleNewNotification(notification: ReceivedNotification)
    suspend fun generateSummaryIfNeeded(senderId: Long): Result<Unit>

    fun getAllSendersStream(): Flow<List<SenderEntity>>
    fun getSummariesStream(senderId: Long): Flow<List<SummaryEntity>>
    fun getNotificationsStream(senderId: Long): Flow<List<NotificationEntity>>
    fun getAllNotesStream(): Flow<List<NoteEntity>>
    fun getNotesByScopeStream(scope: String): Flow<List<NoteEntity>>
    suspend fun getNoteBySenderId(senderId: Long): NoteEntity?

    suspend fun mergeSenders(oldSenderId: Long, newSenderId: Long)
}
