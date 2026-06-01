package com.example.openglow.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "personalization_rules",
    indices = [
        Index(value = ["ruleType", "pattern"], unique = true),
    ],
)
data class PersonalizationRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ruleType: String,
    val pattern: String,
    val importanceDelta: Int,
    val workRelatedBias: Int,
    val senderScopeOverride: String?,
    val source: String,
    val confidence: Float,
    val updatedAt: Long,
)
