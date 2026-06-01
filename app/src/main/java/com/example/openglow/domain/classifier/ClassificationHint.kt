package com.example.openglow.domain.classifier

data class ClassificationHint(
    val workRelatedScore: Float,
    val importanceHint: String?,
    val meetingScore: Float,
    val projectScore: Float,
    val confidence: Float,
    val modelName: String,
)
