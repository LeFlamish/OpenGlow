package com.example.openglow.domain.llm

import com.google.gson.Gson
import com.google.gson.Strictness
import com.google.gson.stream.JsonReader
import java.io.StringReader
import java.util.Locale

object AnalysisJsonParser {
    fun parse(
        gson: Gson,
        responseText: String,
        fallbackInput: NoteUpdateInput,
        modelSource: ModelSource,
    ): Result<NotificationAnalysisResult> {
        val json = responseText.extractJsonObject()
            ?: return Result.failure(IllegalArgumentException("LLM response did not contain a JSON object"))

        return runCatching {
            val reader = JsonReader(StringReader(json)).apply { strictness = Strictness.LENIENT }
            val payload = gson.fromJson<AnalysisPayload>(reader, AnalysisPayload::class.java)
                ?: error("LLM response JSON was empty")
            payload.toResult(fallbackInput, modelSource).validated(fallbackInput)
        }
    }

    /**
     * Extracts the first complete JSON object, tolerating surrounding prose or markdown code
     * fences. Scans from the first '{' to its balanced closing '}', ignoring braces inside
     * string literals so any trailing text or a second object is dropped.
     */
    private fun String.extractJsonObject(): String? {
        val start = indexOf('{')
        if (start == -1) return null
        var depth = 0
        var inString = false
        var escaped = false
        for (i in start until length) {
            val c = this[i]
            if (inString) {
                when {
                    escaped -> escaped = false
                    c == '\\' -> escaped = true
                    c == '"' -> inString = false
                }
            } else {
                when (c) {
                    '"' -> inString = true
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) return substring(start, i + 1)
                    }
                }
            }
        }
        return null
    }

    private fun AnalysisPayload.toResult(
        input: NoteUpdateInput,
        modelSource: ModelSource,
    ): NotificationAnalysisResult {
        val summary = oneLineSummary.orEmpty().ifBlank {
            updatedFinalSummary.orEmpty().lineSequence().firstOrNull()?.take(100).orEmpty()
        }.ifBlank {
            input.newNotificationText.take(100)
        }

        return NotificationAnalysisResult(
            oneLineSummary = summary,
            importance = importance.toImportance(),
            isWorkRelated = isWorkRelated ?: false,
            senderScope = senderScope.toSenderScope() ?: input.senderScope,
            noteTitle = noteTitle.orEmpty().ifBlank { input.senderDisplayName },
            updatedFinalSummary = updatedFinalSummary.orEmpty().ifBlank { summary },
            retainedFacts = retainedFacts.orEmpty().mapNotNull { it.clean() }.take(10),
            actionItems = actionItems.orEmpty().mapNotNull { it.clean() }.take(10),
            deadlineText = deadlineText?.clean(),
            meetingDetected = meetingDetected ?: false,
            projectDetected = projectDetected ?: false,
            shouldAskFeedback = shouldAskFeedback ?: ((confidence ?: 0.5f) < 0.7f),
            confidence = (confidence ?: 0.5f).coerceIn(0f, 1f),
            modelSource = modelSource,
        )
    }

    private fun NotificationAnalysisResult.validated(input: NoteUpdateInput): NotificationAnalysisResult {
        val finalSummary = FinalSummarySanitizer.sanitize(updatedFinalSummary)
            .ifBlank { oneLineSummary }
            .limit(1200)
        val title = noteTitle.ifBlank { input.senderDisplayName }.limit(80)

        return copy(
            oneLineSummary = oneLineSummary.ifBlank { finalSummary.take(120) }.limit(180),
            noteTitle = title,
            updatedFinalSummary = finalSummary,
            retainedFacts = retainedFacts.filter { it.isNotBlank() }.distinct().take(10),
            actionItems = actionItems.filter { it.isNotBlank() }.distinct().take(10),
            confidence = confidence.coerceIn(0f, 1f),
        )
    }

    private fun String?.toImportance(): ImportanceLevel {
        return runCatching {
            ImportanceLevel.valueOf(orEmpty().uppercase(Locale.US))
        }.getOrDefault(ImportanceLevel.NORMAL)
    }

    private fun String?.toSenderScope(): SenderScope? {
        if (isNullOrBlank()) return null
        return runCatching { SenderScope.valueOf(uppercase(Locale.US)) }.getOrNull()
    }

    private fun String.clean(): String? {
        return replace(Regex("\\s+"), " ")
            .trim()
            .takeIf { it.isNotBlank() }
    }

    private fun String.limit(max: Int): String {
        return if (length <= max) this else take(max).trimEnd()
    }

    private data class AnalysisPayload(
        val oneLineSummary: String? = null,
        val importance: String? = null,
        val isWorkRelated: Boolean? = null,
        val senderScope: String? = null,
        val noteTitle: String? = null,
        val updatedFinalSummary: String? = null,
        val retainedFacts: List<String>? = null,
        val actionItems: List<String>? = null,
        val deadlineText: String? = null,
        val meetingDetected: Boolean? = null,
        val projectDetected: Boolean? = null,
        val shouldAskFeedback: Boolean? = null,
        val confidence: Float? = null,
    )
}
