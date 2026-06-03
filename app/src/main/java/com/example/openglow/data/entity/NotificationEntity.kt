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
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["identifierId"]),
        Index(value = ["timestamp"]),
    ],
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val identifierId: Long,
    val senderName: String?,
    val packageName: String,
    val content: String,
    val textFragmentsJson: String,
    val completenessConfidence: Float,
    val isLikelyComplete: Boolean,
    val timestamp: Long,
    val isSummarized: Boolean = false,
<<<<<<< Updated upstream
=======
    val isRagUploaded: Boolean = false,
    val ragUploadedAt: Long? = null,
    val ragRemoteDocumentId: String? = null
>>>>>>> Stashed changes
)
