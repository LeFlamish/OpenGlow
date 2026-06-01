package com.example.openglow.data.remote.dto

data class GeminiGenerateContentRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig = GeminiGenerationConfig(),
)

data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>,
)

data class GeminiPart(
    val text: String,
)

data class GeminiGenerationConfig(
    val responseMimeType: String = "application/json",
    val responseSchema: Map<String, Any> = notificationAnalysisSchema(),
)

data class GeminiGenerateContentResponse(
    val candidates: List<GeminiCandidate>? = emptyList(),
)

data class GeminiCandidate(
    val content: GeminiContent? = null,
)

private fun notificationAnalysisSchema(): Map<String, Any> = mapOf(
    "type" to "object",
    "properties" to mapOf(
        "oneLineSummary" to mapOf("type" to "string"),
        "importance" to mapOf(
            "type" to "string",
            "enum" to listOf("LOW", "NORMAL", "HIGH", "URGENT"),
        ),
        "isWorkRelated" to mapOf("type" to "boolean"),
        "senderScope" to mapOf(
            "type" to "string",
            "enum" to listOf("INDIVIDUAL", "GROUP", "UNKNOWN"),
        ),
        "noteTitle" to mapOf("type" to "string"),
        "updatedFinalSummary" to mapOf("type" to "string"),
        "retainedFacts" to mapOf(
            "type" to "array",
            "items" to mapOf("type" to "string"),
        ),
        "actionItems" to mapOf(
            "type" to "array",
            "items" to mapOf("type" to "string"),
        ),
        "deadlineText" to mapOf("type" to listOf("string", "null")),
        "meetingDetected" to mapOf("type" to "boolean"),
        "projectDetected" to mapOf("type" to "boolean"),
        "shouldAskFeedback" to mapOf("type" to "boolean"),
        "confidence" to mapOf("type" to "number"),
    ),
    "required" to listOf(
        "oneLineSummary",
        "importance",
        "isWorkRelated",
        "senderScope",
        "noteTitle",
        "updatedFinalSummary",
        "retainedFacts",
        "actionItems",
        "deadlineText",
        "meetingDetected",
        "projectDetected",
        "shouldAskFeedback",
        "confidence",
    ),
)
