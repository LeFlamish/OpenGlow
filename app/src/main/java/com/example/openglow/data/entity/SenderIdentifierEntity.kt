package com.example.openglow.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class IdentifierType {
    UNIQUE_ID,    // 앱에서 제공하는 고유 방 ID, 이메일 등
    DISPLAY_NAME, // 단순 이름 문자열 (이름 기반 매핑 시 사용)
    PHONE_NUMBER  // 전화번호
}

@Entity(
    tableName = "sender_identifiers",
    indices = [
        Index(value = ["platform", "identifierValue", "identifierType"], unique = true),
        Index(value = ["identifierValue"], unique = true) // FK 참조를 위해 추가
    ],
    foreignKeys = [
        ForeignKey(
            entity = SenderEntity::class,
            parentColumns = ["id"],
            childColumns = ["senderId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SenderIdentifierEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val senderId: Long,
    val platform: String,          // KAKAO, SMS, EMAIL 등
    val identifierValue: String,   // 고유 ID 또는 이름 문자열
    val identifierType: IdentifierType
)
