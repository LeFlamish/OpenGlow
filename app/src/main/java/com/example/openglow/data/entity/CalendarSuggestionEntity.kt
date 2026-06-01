package com.example.openglow.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "calendar_suggestions",
    indices = [
        Index(value = ["noteId"]),
        Index(value = ["analysisLogId"]),
        Index(value = ["status"]),
    ],
)
data class CalendarSuggestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long,
    val analysisLogId: Long?,
    val title: String,
    val description: String,
    val deadlineText: String?,
    val sourceSummary: String,
    val status: String = "PENDING",
    val createdAt: Long,
    val resolvedAt: Long? = null,
)
