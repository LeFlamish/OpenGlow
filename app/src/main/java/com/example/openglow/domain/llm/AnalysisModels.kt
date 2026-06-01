package com.example.openglow.domain.llm

import com.example.openglow.domain.classifier.ClassificationHint

enum class SenderScope {
    INDIVIDUAL,
    GROUP,
    UNKNOWN,
}

enum class ImportanceLevel {
    LOW,
    NORMAL,
    HIGH,
    URGENT,
}

enum class ModelSource {
    GEMINI,
    LOCAL_LLM,
    RULE_FALLBACK,
}

data class NotificationAnalysisResult(
    val oneLineSummary: String,
    val importance: ImportanceLevel,
    val isWorkRelated: Boolean,
    val senderScope: SenderScope,
    val noteTitle: String,
    val updatedFinalSummary: String,
    val retainedFacts: List<String>,
    val actionItems: List<String>,
    val deadlineText: String?,
    val meetingDetected: Boolean,
    val projectDetected: Boolean,
    val shouldAskFeedback: Boolean,
    val confidence: Float,
    val modelSource: ModelSource,
)

data class NoteUpdateInput(
    val platform: String,
    val packageName: String,
    val senderName: String?,
    val senderDisplayName: String,
    val senderScope: SenderScope,
    val previousFinalSummary: String?,
    val previousRetainedFacts: List<String>,
    val newNotificationText: String,
    val textFragments: List<String>,
    val completenessConfidence: Float,
    val classificationHint: ClassificationHint?,
    val timestamp: Long,
    val userPersonalizationRules: PersonalizationRules,
)

data class PersonalizationRules(
    val rules: List<PersonalizationRule> = emptyList(),
) {
    override fun toString(): String {
        if (rules.isEmpty()) return "none"
        return rules.joinToString(separator = "\n") { rule ->
            "${rule.ruleType}:${rule.pattern} importanceDelta=${rule.importanceDelta} " +
                "workRelatedBias=${rule.workRelatedBias} scope=${rule.senderScopeOverride ?: "none"}"
        }
    }
}

data class PersonalizationRule(
    val ruleType: String,
    val pattern: String,
    val importanceDelta: Int,
    val workRelatedBias: Int,
    val senderScopeOverride: SenderScope?,
    val confidence: Float,
)
