package com.example.openglow.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SenderType {
    INDIVIDUAL,
    GROUP,
    UNKNOWN,
}

@Entity(tableName = "senders")
data class SenderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,       // 대표 이름 (예: 철수, 동창회 단톡방)
    val type: SenderType,          // INDIVIDUAL, GROUP
    val lastNotifiedAt: Long       // 마지막 알림 수신 시간
)
