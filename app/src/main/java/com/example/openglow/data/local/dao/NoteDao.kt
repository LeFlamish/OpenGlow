package com.example.openglow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.example.openglow.data.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

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
}
