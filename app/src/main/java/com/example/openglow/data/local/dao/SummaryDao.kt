package com.example.openglow.data.local.dao

import androidx.room.*
import com.example.openglow.data.entity.SummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SummaryDao {
    // LLM API 통신이 성공적으로 끝나고 만들어진 새로운 요약본을 저장
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summary: SummaryEntity): Long

    // [상세 화면용] 특정 발신자(senderId)의 모든 요약 기록 가져오기 (최신순 정렬)
    // 반환 타입이 Flow이므로, DB에 새 요약이 추가되면 UI가 자동으로 새로고침 됨
    @Query("SELECT * FROM summaries WHERE senderId = :senderId ORDER BY createdAt DESC")
    fun getSummariesBySenderFlow(senderId: Long): Flow<List<SummaryEntity>>

    // [목록 화면용] 특정 발신자의 '가장 최근 요약본' 딱 1개만 가져오기
    // 카톡 메인 화면처럼 전체 발신자 리스트를 보여줄 때 '미리보기' 용도로 사용하기 아주 좋음
    @Query("SELECT * FROM summaries WHERE senderId = :senderId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestSummaryBySender(senderId: Long): SummaryEntity?

    @Query("UPDATE summaries SET senderId = :newSenderId WHERE senderId = :oldSenderId")
    suspend fun mergeSummariesToNewSender(oldSenderId: Long, newSenderId: Long)

    @Query("DELETE FROM summaries WHERE senderId = :senderId")
    suspend fun deleteSummariesBySender(senderId: Long)

    // 오래된 요약 기록 지우기
    // thresholdTime: 기준 시간 (예: 30일 전)
    @Query("DELETE FROM summaries WHERE createdAt < :thresholdTime")
    suspend fun deleteOldSummaries(thresholdTime: Long)
}
