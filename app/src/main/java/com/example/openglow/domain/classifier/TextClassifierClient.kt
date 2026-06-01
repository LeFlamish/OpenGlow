package com.example.openglow.domain.classifier

interface TextClassifierClient {
    suspend fun isAvailable(): Boolean
    suspend fun classify(text: String): ClassificationHint
}
