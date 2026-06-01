package com.example.openglow.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class IdentifierType {
    UNIQUE_ID,
    DISPLAY_NAME,
    PHONE_NUMBER,
}

@Entity(
    tableName = "sender_identifiers",
    indices = [
        Index(value = ["platform", "identifierValue", "identifierType"], unique = true),
        Index(value = ["senderId"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = SenderEntity::class,
            parentColumns = ["id"],
            childColumns = ["senderId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class SenderIdentifierEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val senderId: Long,
    val platform: String,
    val identifierValue: String,
    val identifierType: IdentifierType,
)
