package com.example.openglow.data.local.dao

import androidx.room.*
import com.example.openglow.data.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

data class RagNotificationRow(
    val notificationId: Long,
    val identifierId: Long,
    val senderId: Long,
    val senderName: String?,
    val senderDisplayName: String,
    val platform: String,
    val identifierValue: String,
    val packageName: String,
    val content: String,
    val timestamp: Long,
    val isSummarized: Boolean,
    val isRagUploaded: Boolean
)

@Dao
interface NotificationDao {
    // 새 알림 저장 (백그라운드 알림 수신 시 호출)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): Long

    // [핵심 로직] 특정 통합 발신자(senderId)의 '아직 요약되지 않은' 모든 알림 가져오기
    // 성능 최적화를 위해 문자열(String)이 아닌 숫자 식별자(identifierId)로 JOIN을 수행
    @Query("""
        SELECT n.* FROM notifications n 
        INNER JOIN sender_identifiers i ON n.identifierId = i.id 
        WHERE i.senderId = :senderId AND n.isSummarized = 0 
        ORDER BY n.timestamp ASC
    """)
    suspend fun getUnsummarizedNotificationsBySender(senderId: Long): List<NotificationEntity>

    // 특정 발신자의 모든 알림(최신순) 가져오기 - UI용
    @Query("""
        SELECT n.* FROM notifications n 
        INNER JOIN sender_identifiers i ON n.identifierId = i.id 
        WHERE i.senderId = :senderId
        ORDER BY n.timestamp DESC
    """)
    fun getNotificationsBySenderFlow(senderId: Long): Flow<List<NotificationEntity>>

    // 요약 완료 처리 (LLM API 통신 성공 후 호출)
    // 전달받은 알림 ID들의 isSummarized 플래그를 true(1)로 변경
    @Query("UPDATE notifications SET isSummarized = 1 WHERE id IN (:notificationIds)")
    suspend fun markAsSummarized(notificationIds: List<Long>)

    // 오래된 '요약 완료' 알림 삭제
    // thresholdTime: 기준이 되는 과거 시간 (예: 7일 전)
    @Query("DELETE FROM notifications WHERE isSummarized = 1 AND timestamp < :thresholdTime")
    suspend fun deleteOldSummarizedNotifications(thresholdTime: Long)

    @Query("""
        SELECT
            n.id AS notificationId,
            n.identifierId AS identifierId,
            s.id AS senderId,
            n.senderName AS senderName,
            s.displayName AS senderDisplayName,
            si.platform AS platform,
            si.identifierValue AS identifierValue,
            n.packageName AS packageName,
            n.content AS content,
            n.timestamp AS timestamp,
            n.isSummarized AS isSummarized,
            n.isRagUploaded AS isRagUploaded
        FROM notifications n
        INNER JOIN sender_identifiers si ON n.identifierId = si.id
        INNER JOIN senders s ON si.senderId = s.id
        WHERE n.isRagUploaded = 0
        ORDER BY n.timestamp ASC
    """)
    suspend fun getNotUploadedNotificationsForRag(): List<RagNotificationRow>

    @Query("SELECT COUNT(*) FROM notifications")
    suspend fun getTotalNotificationCount(): Int

    @Query("SELECT COUNT(*) FROM notifications WHERE isRagUploaded = 1")
    suspend fun getRagUploadedNotificationCount(): Int

    @Query("SELECT COUNT(*) FROM notifications WHERE isRagUploaded = 0")
    suspend fun getRagNotUploadedNotificationCount(): Int

    @Query("""
        UPDATE notifications
        SET isRagUploaded = 1,
            ragUploadedAt = :uploadedAt,
            ragRemoteDocumentId = :remoteDocumentId
        WHERE id = :notificationId
    """)
    suspend fun markNotificationAsRagUploaded(
        notificationId: Long,
        uploadedAt: Long,
        remoteDocumentId: String
    )
}
