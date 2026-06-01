package com.example.openglow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.openglow.data.entity.FeedbackEntity

@Dao
interface FeedbackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: FeedbackEntity): Long

    @Query("SELECT * FROM feedback ORDER BY createdAt DESC")
    suspend fun getAllFeedback(): List<FeedbackEntity>

    @Query("SELECT * FROM feedback WHERE noteId = :noteId ORDER BY createdAt DESC")
    suspend fun getFeedbackByNote(noteId: Long): List<FeedbackEntity>
}
