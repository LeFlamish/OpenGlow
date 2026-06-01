package com.example.openglow.domain.llm

import android.util.Log
import com.example.openglow.data.localai.LocalLlmClient
import com.example.openglow.data.remote.GeminiLlmClient
import com.example.openglow.domain.personalization.PersonalizationEngine
import com.google.gson.Gson
import javax.inject.Inject

class LlmRouter @Inject constructor(
    private val geminiLlmClient: GeminiLlmClient,
    private val localLlmClient: LocalLlmClient,
    private val ruleBasedAnalyzer: RuleBasedAnalyzer,
    private val personalizationEngine: PersonalizationEngine,
    private val gson: Gson,
) {
    suspend fun analyzeAndUpdateNote(input: NoteUpdateInput): NotificationAnalysisResult {
        if (input.newNotificationText.isBlank()) {
            return ruleFallback(input, "blank_text")
        }

        val geminiResult = geminiLlmClient.analyze(input)
        if (geminiResult.isSuccess) {
            return personalizationEngine.postProcess(input, geminiResult.getOrThrow())
        }

        Log.w(TAG, "Gemini fallback reason: ${geminiResult.exceptionOrNull()?.message}")

        val localResult = runLocalFallback(input)
        if (localResult.isSuccess) {
            return personalizationEngine.postProcess(input, localResult.getOrThrow())
        }

        Log.w(TAG, "Local fallback reason: ${localResult.exceptionOrNull()?.message}")
        return personalizationEngine.postProcess(input, ruleFallback(input, "local_failed"))
    }

    private suspend fun runLocalFallback(input: NoteUpdateInput): Result<NotificationAnalysisResult> {
        if (!localLlmClient.isAvailable()) {
            return Result.failure(IllegalStateException("Local LLM unavailable"))
        }

        return localLlmClient.analyze(input).mapCatching { responseText ->
            AnalysisJsonParser.parse(
                gson = gson,
                responseText = responseText,
                fallbackInput = input,
                modelSource = ModelSource.LOCAL_LLM,
            ).getOrThrow()
        }
    }

    private fun ruleFallback(input: NoteUpdateInput, reason: String): NotificationAnalysisResult {
        Log.i(TAG, "Using rule fallback: reason=$reason, textLength=${input.newNotificationText.length}")
        return ruleBasedAnalyzer.analyze(input).copy(modelSource = ModelSource.RULE_FALLBACK)
    }

    private companion object {
        private const val TAG = "LlmRouter"
    }
}
