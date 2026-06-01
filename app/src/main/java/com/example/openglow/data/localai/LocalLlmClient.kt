package com.example.openglow.data.localai

import com.example.openglow.domain.llm.NoteUpdateInput

interface LocalLlmClient {
    suspend fun isAvailable(): Boolean
    suspend fun analyze(input: NoteUpdateInput): Result<String>
}
