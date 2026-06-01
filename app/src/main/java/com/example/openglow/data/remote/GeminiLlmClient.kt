package com.example.openglow.data.remote

import android.util.Log
import com.example.openglow.BuildConfig
import com.example.openglow.data.remote.dto.GeminiContent
import com.example.openglow.data.remote.dto.GeminiGenerateContentRequest
import com.example.openglow.data.remote.dto.GeminiGenerateContentResponse
import com.example.openglow.data.remote.dto.GeminiPart
import com.example.openglow.domain.llm.AnalysisJsonParser
import com.example.openglow.domain.llm.AnalysisPromptBuilder
import com.example.openglow.domain.llm.ModelSource
import com.example.openglow.domain.llm.NoteUpdateInput
import com.example.openglow.domain.llm.NotificationAnalysisResult
import com.google.gson.Gson
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import retrofit2.HttpException

class GeminiLlmClient @Inject constructor(
    private val geminiApiService: GeminiApiService,
    private val gson: Gson,
) {
    suspend fun analyze(input: NoteUpdateInput): Result<NotificationAnalysisResult> {
        if (BuildConfig.GEMINI_API_KEY.isBlank()) {
            return Result.failure(GeminiLlmException(GeminiFailureReason.API_KEY_MISSING, "Gemini API key missing"))
        }

        return runCatching {
            Log.i(TAG, "Gemini request started: model=${BuildConfig.GEMINI_MODEL}, textLength=${input.newNotificationText.length}")
            val response = geminiApiService.generateContent(
                model = BuildConfig.GEMINI_MODEL,
                apiKey = BuildConfig.GEMINI_API_KEY,
                request = GeminiGenerateContentRequest(
                    contents = listOf(
                        GeminiContent(
                            role = "user",
                            parts = listOf(GeminiPart(AnalysisPromptBuilder.buildGeminiPrompt(input))),
                        ),
                    ),
                ),
            )

            val responseText = response.extractText()
                ?: throw GeminiLlmException(GeminiFailureReason.EMPTY_RESPONSE, "Gemini response text was empty")

            AnalysisJsonParser.parse(
                gson = gson,
                responseText = responseText,
                fallbackInput = input,
                modelSource = ModelSource.GEMINI,
            ).getOrElse {
                throw GeminiLlmException(GeminiFailureReason.JSON_PARSE_FAILED, it.message ?: "Gemini JSON parse failed", it)
            }
        }.recoverCatching { throwable ->
            throw classifyFailure(throwable)
        }.onSuccess {
            Log.i(TAG, "Gemini analysis succeeded: importance=${it.importance}, confidence=${it.confidence}")
        }.onFailure {
            Log.w(TAG, "Gemini analysis failed: ${it.message}")
        }
    }

    private fun GeminiGenerateContentResponse.extractText(): String? {
        return candidates.orEmpty()
            .firstOrNull()
            ?.content
            ?.parts
            .orEmpty()
            .joinToString(separator = "\n") { it.text }
            .trim()
            .takeIf { it.isNotBlank() }
    }

    private fun classifyFailure(throwable: Throwable): GeminiLlmException {
        if (throwable is GeminiLlmException) return throwable

        val message = throwable.message.orEmpty()
        val lower = message.lowercase()

        return when {
            throwable is HttpException && throwable.code() == 429 ->
                GeminiLlmException(GeminiFailureReason.RATE_LIMITED, "Gemini rate limited: 429", throwable)
            throwable is HttpException && throwable.code() == 401 ->
                GeminiLlmException(GeminiFailureReason.INVALID_API_KEY, "Gemini invalid API key", throwable)
            throwable is HttpException && throwable.code() == 403 ->
                GeminiLlmException(GeminiFailureReason.QUOTA_OR_PERMISSION, "Gemini quota or permission error", throwable)
            "resource_exhausted" in lower || "quota" in lower ->
                GeminiLlmException(GeminiFailureReason.QUOTA_OR_PERMISSION, message, throwable)
            "rate limit" in lower ->
                GeminiLlmException(GeminiFailureReason.RATE_LIMITED, message, throwable)
            throwable is UnknownHostException ->
                GeminiLlmException(GeminiFailureReason.NETWORK, "Gemini network unavailable", throwable)
            throwable is SocketTimeoutException ->
                GeminiLlmException(GeminiFailureReason.TIMEOUT, "Gemini timeout", throwable)
            throwable is IOException ->
                GeminiLlmException(GeminiFailureReason.NETWORK, message.ifBlank { "Gemini network error" }, throwable)
            else -> GeminiLlmException(GeminiFailureReason.UNKNOWN, message.ifBlank { "Gemini unknown error" }, throwable)
        }
    }

    private companion object {
        private const val TAG = "GeminiLlmClient"
    }
}

class GeminiLlmException(
    val reason: GeminiFailureReason,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

enum class GeminiFailureReason {
    API_KEY_MISSING,
    INVALID_API_KEY,
    RATE_LIMITED,
    QUOTA_OR_PERMISSION,
    NETWORK,
    TIMEOUT,
    JSON_PARSE_FAILED,
    EMPTY_RESPONSE,
    UNKNOWN,
}
