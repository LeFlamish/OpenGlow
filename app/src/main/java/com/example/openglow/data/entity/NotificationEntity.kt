package com.example.openglow.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notifications",
    foreignKeys = [
        ForeignKey(
            entity = SenderIdentifierEntity::class,
            parentColumns = ["id"],
            childColumns = ["identifierId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["identifierId"])]
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val identifierId: Long,   // SenderIdentifierEntity의 id 참조
    val senderName: String?,        // 실제 발언자 이름
    val packageName: String,
    val content: String,
    val timestamp: Long,
    val isSummarized: Boolean = false
)
