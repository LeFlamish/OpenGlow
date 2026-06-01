package com.example.openglow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.openglow.data.entity.CalendarSuggestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarSuggestionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuggestion(suggestion: CalendarSuggestionEntity): Long

    @Query("SELECT * FROM calendar_suggestions WHERE status = 'PENDING' ORDER BY createdAt DESC")
    fun getPendingSuggestionsFlow(): Flow<List<CalendarSuggestionEntity>>

    @Query("SELECT * FROM calendar_suggestions WHERE status = 'PENDING' ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestPendingSuggestion(): CalendarSuggestionEntity?

    @Query("UPDATE calendar_suggestions SET status = :status, resolvedAt = :resolvedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, resolvedAt: Long)
}
