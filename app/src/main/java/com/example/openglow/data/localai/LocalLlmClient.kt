package com.example.openglow.data.localai

interface LocalLlmClient {
    suspend fun isAvailable(): Boolean
    suspend fun analyze(prompt: String): Result<String>
}
