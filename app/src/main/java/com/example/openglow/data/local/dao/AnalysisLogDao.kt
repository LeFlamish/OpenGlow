package com.example.openglow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.openglow.data.entity.AnalysisLogEntity

@Dao
interface AnalysisLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalysisLog(log: AnalysisLogEntity): Long

    @Query("SELECT * FROM analysis_logs WHERE id = :id LIMIT 1")
    suspend fun getAnalysisLogById(id: Long): AnalysisLogEntity?

    @Query("SELECT * FROM analysis_logs WHERE noteId = :noteId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestAnalysisLogByNoteId(noteId: Long): AnalysisLogEntity?

    @Query(
        """
        SELECT * FROM analysis_logs
        WHERE feedbackResolved = 0
        ORDER BY
            CASE WHEN confidence < 0.7 THEN 0 ELSE 1 END,
            CASE WHEN importance IN ('URGENT', 'HIGH') THEN 0 ELSE 1 END,
            createdAt DESC
        LIMIT :limit
        """,
    )
    suspend fun getPendingFeedbackCandidates(limit: Int): List<AnalysisLogEntity>

    @Query("UPDATE analysis_logs SET feedbackRequested = 1 WHERE id IN (:ids)")
    suspend fun markFeedbackRequested(ids: List<Long>)

    @Query("UPDATE analysis_logs SET feedbackResolved = 1 WHERE id = :id")
    suspend fun markFeedbackResolved(id: Long)
}
