package com.example.openglow.domain.personalization

import android.util.Log
import com.example.openglow.data.entity.AnalysisLogEntity
import com.example.openglow.data.entity.FeedbackEntity
import com.example.openglow.data.entity.NoteEntity
import com.example.openglow.data.entity.PersonalizationRuleEntity
import com.example.openglow.data.local.dao.PersonalizationRuleDao
import com.example.openglow.domain.llm.ImportanceLevel
import com.example.openglow.domain.llm.NoteUpdateInput
import com.example.openglow.domain.llm.NotificationAnalysisResult
import com.example.openglow.domain.llm.PersonalizationRule
import com.example.openglow.domain.llm.PersonalizationRules
import com.example.openglow.domain.llm.SenderScope
import java.util.Locale
import javax.inject.Inject

class PersonalizationEngine @Inject constructor(
    private val ruleDao: PersonalizationRuleDao,
) {
    suspend fun loadRules(): PersonalizationRules {
        val rules = ruleDao.getAllRules().map { entity ->
            PersonalizationRule(
                ruleType = entity.ruleType,
                pattern = entity.pattern,
                importanceDelta = entity.importanceDelta,
                workRelatedBias = entity.workRelatedBias,
                senderScopeOverride = entity.senderScopeOverride?.let {
                    runCatching { SenderScope.valueOf(it) }.getOrNull()
                },
                confidence = entity.confidence,
            )
        }
        return PersonalizationRules(rules)
    }

    fun postProcess(
        input: NoteUpdateInput,
        result: NotificationAnalysisResult,
    ): NotificationAnalysisResult {
        val matched = input.userPersonalizationRules.rules.filter { rule ->
            matches(rule, input)
        }
        if (matched.isEmpty()) return result

        val importanceDelta = matched.sumOf { it.importanceDelta }.coerceIn(-2, 2)
        val workBias = matched.sumOf { it.workRelatedBias }.coerceIn(-2, 2)
        val scopeOverride = matched.firstNotNullOfOrNull { it.senderScopeOverride }

        val adjusted = result.copy(
            importance = adjustImportance(result.importance, importanceDelta),
            isWorkRelated = when {
                workBias > 0 -> true
                workBias < 0 -> false
                else -> result.isWorkRelated
            },
            senderScope = scopeOverride ?: result.senderScope,
            confidence = (result.confidence + 0.05f).coerceAtMost(1f),
        )

        Log.i(
            TAG,
            "Personalization applied: rules=${matched.size}, importance=${result.importance}->${adjusted.importance}, " +
                "work=${result.isWorkRelated}->${adjusted.isWorkRelated}",
        )
        return adjusted
    }

    suspend fun recordFeedback(
        feedback: FeedbackEntity,
        analysisLog: AnalysisLogEntity,
        note: NoteEntity?,
    ) {
        val now = System.currentTimeMillis()
        val senderPattern = note?.senderDisplayName?.normalizePattern()

        if (senderPattern != null) {
            val importanceDelta = feedback.correctedImportance?.let {
                importanceDelta(feedback.originalImportance, it)
            } ?: 0
            val workBias = feedback.correctedIsWorkRelated?.let {
                if (it) 1 else -1
            } ?: 0

            val scopeOverride = feedback.correctedSenderScope
                ?.takeIf { it != feedback.originalSenderScope }

            upsertRule(
                rule = PersonalizationRuleEntity(
                    ruleType = "sender",
                    pattern = senderPattern,
                    importanceDelta = importanceDelta,
                    workRelatedBias = workBias,
                    senderScopeOverride = scopeOverride,
                    source = "feedback:${feedback.id}:${analysisLog.id}",
                    confidence = 0.7f,
                    updatedAt = now,
                ),
            )
        }

        feedback.userComment
            ?.split(Regex("[\\s,.;:/]+"))
            .orEmpty()
            .map { it.trim().normalizePattern() }
            .filter { it.length >= 2 }
            .distinct()
            .take(5)
            .forEach { keyword ->
                upsertRule(
                    rule = PersonalizationRuleEntity(
                        ruleType = "keyword",
                        pattern = keyword,
                        importanceDelta = feedback.correctedImportance?.let {
                            importanceDelta(feedback.originalImportance, it)
                        } ?: 0,
                        workRelatedBias = feedback.correctedIsWorkRelated?.let { if (it) 1 else -1 } ?: 0,
                        senderScopeOverride = feedback.correctedSenderScope
                            ?.takeIf { it != feedback.originalSenderScope },
                        source = "feedback:${feedback.id}:${analysisLog.id}",
                        confidence = 0.55f,
                        updatedAt = now,
                    ),
                )
            }
    }

    private suspend fun upsertRule(rule: PersonalizationRuleEntity) {
        val existing = ruleDao.getRule(rule.ruleType, rule.pattern)
        if (existing == null) {
            ruleDao.insertRule(rule)
        } else {
            ruleDao.updateRule(
                existing.copy(
                    importanceDelta = (existing.importanceDelta + rule.importanceDelta).coerceIn(-3, 3),
                    workRelatedBias = (existing.workRelatedBias + rule.workRelatedBias).coerceIn(-3, 3),
                    senderScopeOverride = rule.senderScopeOverride ?: existing.senderScopeOverride,
                    source = rule.source,
                    confidence = maxOf(existing.confidence, rule.confidence).coerceAtMost(1f),
                    updatedAt = rule.updatedAt,
                ),
            )
        }
    }

    private fun matches(rule: PersonalizationRule, input: NoteUpdateInput): Boolean {
        val pattern = rule.pattern.normalizePattern()
        return when (rule.ruleType) {
            "sender" -> input.senderDisplayName.normalizePattern() == pattern
            "keyword" -> input.newNotificationText.normalizePattern().contains(pattern)
            else -> false
        }
    }

    private fun adjustImportance(importance: ImportanceLevel, delta: Int): ImportanceLevel {
        val values = ImportanceLevel.entries
        return values[(importance.ordinal + delta).coerceIn(0, values.lastIndex)]
    }

    private fun importanceDelta(original: String, corrected: String): Int {
        val originalLevel = runCatching { ImportanceLevel.valueOf(original) }.getOrDefault(ImportanceLevel.NORMAL)
        val correctedLevel = runCatching { ImportanceLevel.valueOf(corrected) }.getOrDefault(originalLevel)
        return (correctedLevel.ordinal - originalLevel.ordinal).coerceIn(-2, 2)
    }

    private fun String.normalizePattern(): String {
        return lowercase(Locale.KOREA).trim()
    }

    private companion object {
        private const val TAG = "PersonalizationEngine"
    }
}
