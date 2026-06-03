package com.example.openglow.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["senderId"]),
        Index(value = ["senderScope"]),
        Index(value = ["platform"]),
        Index(value = ["updatedAt"]),
    ],
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val senderId: Long,
    val platform: String,
    val senderDisplayName: String,
    val senderScope: String,
    val title: String,
    val finalSummary: String,
    val retainedFactsJson: String,
    val latestOneLineSummary: String,
    val latestImportance: String,
    val latestIsWorkRelated: Boolean,
    val latestMeetingDetected: Boolean,
    val latestProjectDetected: Boolean,
    val aggregateImportance: String,
    val aggregateIsWorkRelated: Boolean,
    val latestActionItemsJson: String,
    val latestDeadlineText: String?,
    val calendarCandidate: Boolean,
    val notificationCount: Int,
    val modelSource: String,
    val confidence: Float,
    val createdAt: Long,
    val updatedAt: Long,
    val ragUploadedAt: Long? = null,
    val ragRemoteDocumentId: String? = null,
    val ragLastUploadedNoteUpdatedAt: Long? = null,
)
