package com.example.openglow.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "feedback",
    indices = [
        Index(value = ["analysisLogId"]),
        Index(value = ["noteId"]),
    ],
)
data class FeedbackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val analysisLogId: Long,
    val noteId: Long,
    val originalImportance: String,
    val correctedImportance: String?,
    val originalIsWorkRelated: Boolean,
    val correctedIsWorkRelated: Boolean?,
    val originalSenderScope: String,
    val correctedSenderScope: String?,
    val userComment: String?,
    val createdAt: Long,
)
