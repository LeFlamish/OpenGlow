package com.example.openglow.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "analysis_logs",
    indices = [
        Index(value = ["notificationId"]),
        Index(value = ["noteId"]),
        Index(value = ["senderId"]),
        Index(value = ["feedbackRequested", "feedbackResolved"]),
    ],
)
data class AnalysisLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val notificationId: Long,
    val noteId: Long,
    val senderId: Long,
    val inputTextPreview: String,
    val inputTextHash: String,
    val oneLineSummary: String,
    val importance: String,
    val isWorkRelated: Boolean,
    val meetingDetected: Boolean,
    val projectDetected: Boolean,
    val senderScope: String,
    val modelSource: String,
    val confidence: Float,
    val createdAt: Long,
    val feedbackRequested: Boolean = false,
    val feedbackResolved: Boolean = false,
)
