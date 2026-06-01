package com.example.openglow.domain.llm

object FinalSummarySanitizer {
    private val forbiddenLabels = listOf(
        "최근:",
        "보낸사람:",
        "보낸 사람:",
        "이전 정리:",
        "보존할 사실:",
        "기존 정리:",
        "Previous summary:",
        "Retained facts:",
        "Recent:",
    )

    fun sanitize(text: String): String {
        return text
            .lineSequence()
            .map { line ->
                var cleaned = line.trim()
                forbiddenLabels.forEach { label ->
                    if (cleaned.startsWith(label, ignoreCase = true)) {
                        cleaned = cleaned.removePrefix(label).trim()
                    }
                }
                cleaned
            }
            .filter { it.isNotBlank() }
            .joinToString(separator = "\n")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
            .take(1200)
    }
}
