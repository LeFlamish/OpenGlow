package com.example.openglow.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "summaries",
    indices = [Index(value = ["senderId"])],
    foreignKeys = [
        ForeignKey(
            entity = SenderEntity::class,
            parentColumns = ["id"],
            childColumns = ["senderId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SummaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val senderId: Long,
    val summarizedText: String,    // 요약 결과
    val importance: String,
    val isWorkRelated: Boolean,
    val noteTitle: String,
    val actionItemsJson: String,
    val deadlineText: String?,
    val createdAt: Long,           // 생성 시점
    val notificationCount: Int     // 요약에 포함된 알림 개수
)
