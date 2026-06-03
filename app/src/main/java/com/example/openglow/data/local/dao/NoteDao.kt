package com.example.openglow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.example.openglow.data.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

data class RagNoteRow(
    val noteId: Long,
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
    val ragUploadedAt: Long?,
    val ragLastUploadedNoteUpdatedAt: Long?,
)

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Upsert
    suspend fun upsertNote(note: NoteEntity)

    @Query("SELECT * FROM notes WHERE senderId = :senderId LIMIT 1")
    suspend fun getNoteBySenderId(senderId: Long): NoteEntity?

    @Query("SELECT * FROM notes WHERE id = :noteId LIMIT 1")
    suspend fun getNoteById(noteId: Long): NoteEntity?

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun getAllNotesFlow(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE senderScope = :scope ORDER BY updatedAt DESC")
    fun getNotesByScopeFlow(scope: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun getRecentNotesForFeedback(limit: Int): List<NoteEntity>

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNote(id: Long)

    @Query("UPDATE notes SET senderId = :newSenderId WHERE senderId = :oldSenderId")
    suspend fun mergeNotesToNewSender(oldSenderId: Long, newSenderId: Long)

    @Query("""
        SELECT
            id AS noteId,
            senderId,
            platform,
            senderDisplayName,
            senderScope,
            title,
            finalSummary,
            retainedFactsJson,
            latestOneLineSummary,
            latestImportance,
            latestIsWorkRelated,
            latestMeetingDetected,
            latestProjectDetected,
            aggregateImportance,
            aggregateIsWorkRelated,
            latestActionItemsJson,
            latestDeadlineText,
            calendarCandidate,
            notificationCount,
            modelSource,
            confidence,
            createdAt,
            updatedAt,
            ragUploadedAt,
            ragLastUploadedNoteUpdatedAt
        FROM notes
        WHERE ragUploadedAt IS NULL
            OR ragLastUploadedNoteUpdatedAt IS NULL
            OR ragLastUploadedNoteUpdatedAt < updatedAt
        ORDER BY updatedAt ASC
    """)
    suspend fun getNotUploadedNotesForRag(): List<RagNoteRow>

    @Query("SELECT COUNT(*) FROM notes")
    suspend fun getTotalNoteCount(): Int

    @Query("""
        SELECT COUNT(*) FROM notes
        WHERE ragUploadedAt IS NOT NULL
            AND ragLastUploadedNoteUpdatedAt IS NOT NULL
            AND ragLastUploadedNoteUpdatedAt >= updatedAt
    """)
    suspend fun getRagUploadedNoteCount(): Int

    @Query("""
        SELECT COUNT(*) FROM notes
        WHERE ragUploadedAt IS NULL
            OR ragLastUploadedNoteUpdatedAt IS NULL
            OR ragLastUploadedNoteUpdatedAt < updatedAt
    """)
    suspend fun getRagNotUploadedNoteCount(): Int

    @Query("""
        UPDATE notes
        SET ragUploadedAt = :uploadedAt,
            ragRemoteDocumentId = :remoteDocumentId,
            ragLastUploadedNoteUpdatedAt = :noteUpdatedAt
        WHERE id = :noteId
    """)
    suspend fun markNoteAsRagUploaded(
        noteId: Long,
        uploadedAt: Long,
        remoteDocumentId: String,
        noteUpdatedAt: Long,
    )
}
