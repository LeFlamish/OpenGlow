package com.example.openglow.data.local.dao

import androidx.room.*
import com.example.openglow.data.entity.IdentifierType
import com.example.openglow.data.entity.SenderEntity
import com.example.openglow.data.entity.SenderIdentifierEntity
import kotlinx.coroutines.flow.Flow

data class SenderWithIdentifier(
    @Embedded val identifier: SenderIdentifierEntity,
    @Relation(
        parentColumn = "senderId", // SenderIdentifierEntity의 외래키
        entityColumn = "id"        // SenderEntity의 기본키(PK)
    )
    val sender: SenderEntity
)

@Dao
interface SenderDao {

    // ==========================================
    // 1. 데이터 삽입 및 갱신 (Insert & Update)
    // ==========================================

    // 새로운 통합 발신자 생성 (생성된 PK 반환)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSender(sender: SenderEntity): Long

    // 발신자 식별자 추가
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIdentifier(identifier: SenderIdentifierEntity): Long

    // 알림 수신 시 발신자의 최근 알림 시간(lastNotifiedAt) 갱신
    @Query("UPDATE senders SET lastNotifiedAt = :timestamp WHERE id = :senderId")
    suspend fun updateLastNotifiedAt(senderId: Long, timestamp: Long)

    // [수동 통합 기능] 특정 식별자들의 소속을 다른 발신자로 변경 (Merge)
    @Query("UPDATE sender_identifiers SET senderId = :newSenderId WHERE senderId = :oldSenderId")
    suspend fun mergeIdentifiersToNewSender(oldSenderId: Long, newSenderId: Long)


    // ==========================================
    // 2. 데이터 조회 (Select)
    // ==========================================

    // UI 표시용: 전체 발신자 목록 가져오기 (최근 알림 순)
    @Query("SELECT * FROM senders ORDER BY lastNotifiedAt DESC")
    fun getAllSendersFlow(): Flow<List<SenderEntity>>

    // ⚡ [가벼운 쿼리] 새 알림 수신 시: 백그라운드에서 식별자 ID만 빠르게 찾을 때 사용
    // 3가지 조건(value, platform, type)을 모두 사용해 유니크 인덱스를 100% 활용
    @Query("""
        SELECT * FROM sender_identifiers 
        WHERE identifierValue = :value 
          AND platform = :platform 
          AND identifierType = :type 
        LIMIT 1
    """)
    suspend fun findIdentifier(
        value: String,
        platform: String,
        type: IdentifierType
    ): SenderIdentifierEntity?

    // [무거운 쿼리] LLM 프롬프트 생성/UI 표시용: 조인을 통해 이름 등 모든 정보를 가져올 때 사용
    // 내부적으로 두 번의 쿼리가 돌기 때문에 데이터 일관성을 위해 @Transaction 필수!
    @Transaction
    @Query("""
        SELECT * FROM sender_identifiers 
        WHERE identifierValue = :value 
          AND platform = :platform 
          AND identifierType = :type 
        LIMIT 1
    """)
    suspend fun findSenderWithIdentifier(
        value: String,
        platform: String,
        type: String
    ): SenderWithIdentifier?

    @Update
    suspend fun updateSender(sender: SenderEntity)

    @Query("DELETE FROM senders WHERE id = :oldSenderId")
    suspend fun deleteSenderById(oldSenderId: Long)
}