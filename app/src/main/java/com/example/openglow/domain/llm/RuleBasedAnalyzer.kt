package com.example.openglow.domain.llm

import android.util.Log
import java.util.Locale
import javax.inject.Inject

class RuleBasedAnalyzer @Inject constructor() {
    fun analyze(input: NoteUpdateInput): NotificationAnalysisResult {
        val text = input.newNotificationText.trim()
        val normalized = text.lowercase(Locale.KOREA)

        val urgent = URGENT_KEYWORDS.any { normalized.contains(it) }
        val workRelated = WORK_KEYWORDS.any { normalized.contains(it) }
        val meetingDetected = MEETING_KEYWORDS.any { normalized.contains(it) }
        val projectDetected = PROJECT_KEYWORDS.any { normalized.contains(it) }
        val lowValue = LOW_VALUE_KEYWORDS.any { normalized.contains(it) }

        val classifierImportance = input.classificationHint?.importanceHint?.let {
            runCatching { ImportanceLevel.valueOf(it) }.getOrNull()
        }
        val importance = when {
            classifierImportance != null && (input.classificationHint.confidence >= 0.7f) -> classifierImportance
            urgent -> ImportanceLevel.URGENT
            workRelated || meetingDetected || projectDetected -> ImportanceLevel.HIGH
            lowValue -> ImportanceLevel.LOW
            else -> ImportanceLevel.NORMAL
        }
        val classifierWorkRelated = (input.classificationHint?.workRelatedScore ?: 0f) >= 0.6f
        val aggregateWorkRelated = workRelated || meetingDetected || projectDetected || classifierWorkRelated

        val oneLine = buildOneLineSummary(text, importance)
        val retainedFacts = extractRetainedFacts(text, input.previousRetainedFacts)
        val finalSummary = mergeSummary(
            previous = input.previousFinalSummary,
            newLine = oneLine,
            retainedFacts = retainedFacts,
        )

        Log.i(
            TAG,
            "Rule fallback result: importance=$importance, work=$workRelated, " +
                "meeting=$meetingDetected, project=$projectDetected, textLength=${text.length}",
        )

        return NotificationAnalysisResult(
            oneLineSummary = oneLine,
            importance = importance,
            isWorkRelated = aggregateWorkRelated,
            senderScope = input.senderScope,
            noteTitle = buildNoteTitle(input, oneLine),
            updatedFinalSummary = finalSummary,
            retainedFacts = retainedFacts,
            actionItems = extractActionItems(text),
            deadlineText = extractDeadline(text),
            meetingDetected = meetingDetected,
            projectDetected = projectDetected,
            shouldAskFeedback = true,
            confidence = if (text.length < 20) 0.46f else 0.62f,
            modelSource = ModelSource.RULE_FALLBACK,
        )
    }

    private fun buildOneLineSummary(text: String, importance: ImportanceLevel): String {
        val compact = text.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() }
            .orEmpty()
            .ifBlank { "내용이 짧은 알림입니다." }
            .limit(140)

        return when (importance) {
            ImportanceLevel.URGENT -> "긴급 확인 필요: $compact"
            ImportanceLevel.HIGH -> "중요 알림: $compact"
            ImportanceLevel.LOW -> "낮은 중요도 알림: $compact"
            ImportanceLevel.NORMAL -> compact
        }.limit(180)
    }

    private fun buildNoteTitle(input: NoteUpdateInput, oneLine: String): String {
        return input.senderDisplayName.ifBlank {
            oneLine.take(32).ifBlank { input.platform }
        }.limit(80)
    }

    private fun mergeSummary(
        previous: String?,
        newLine: String,
        retainedFacts: List<String>,
    ): String {
        val parts = buildList {
            add(newLine)
            if (retainedFacts.isNotEmpty()) {
                add(retainedFacts.joinToString("; "))
            }
            if (!previous.isNullOrBlank()) {
                add(previous.limit(850))
            }
        }

        return parts.joinToString(separator = "\n").limit(1200)
    }

    private fun extractRetainedFacts(text: String, previousFacts: List<String>): List<String> {
        val candidates = text.split('\n', '.', '!', '?')
            .map { it.trim() }
            .filter { line ->
                line.length in 3..180 && RETAIN_KEYWORDS.any { keyword ->
                    line.lowercase(Locale.KOREA).contains(keyword)
                }
            }

        return (candidates + previousFacts)
            .map { it.limit(180) }
            .distinct()
            .take(10)
    }

    private fun extractActionItems(text: String): List<String> {
        val lower = text.lowercase(Locale.KOREA)
        if (ACTION_KEYWORDS.none { lower.contains(it) }) return emptyList()

        return text.split('\n', '.', '!', '?')
            .map { it.trim() }
            .filter { it.length in 3..160 }
            .filter { sentence ->
                ACTION_KEYWORDS.any { sentence.lowercase(Locale.KOREA).contains(it) }
            }
            .map { it.limit(160) }
            .distinct()
            .take(5)
    }

    private fun extractDeadline(text: String): String? {
        val lower = text.lowercase(Locale.KOREA)
        return DEADLINE_PATTERNS.firstOrNull { lower.contains(it) }
            ?: Regex("(오늘|내일|이번 주|다음 주|\\d{1,2}시|\\d{1,2}/\\d{1,2}|\\d{1,2}월\\s*\\d{1,2}일)")
                .find(text)
                ?.value
    }

    private fun String.limit(max: Int): String {
        return if (length <= max) this else take(max).trimEnd()
    }

    private companion object {
        private const val TAG = "RuleBasedAnalyzer"

        private val URGENT_KEYWORDS = listOf(
            "긴급",
            "asap",
            "즉시",
            "오늘까지",
            "곧",
            "마감",
            "deadline",
            "urgent",
        )
        private val WORK_KEYWORDS = listOf(
            "회의",
            "미팅",
            "프로젝트",
            "팀플",
            "과제",
            "발표",
            "보고서",
            "자료",
            "수정",
            "제출",
            "확인",
            "일정",
            "메일",
            "교수",
            "팀장",
            "사수",
            "업무",
            "코드",
            "테스트",
            "regression",
            "배포",
            "zoom",
            "줌",
        )
        private val MEETING_KEYWORDS = listOf("회의", "미팅", "zoom", "줌", "참석", "장소", "시간 변경")
        private val PROJECT_KEYWORDS = listOf("프로젝트", "팀플", "개발", "코드", "보고서", "테스트", "배포", "담당")
        private val LOW_VALUE_KEYWORDS = listOf("광고", "쿠폰", "혜택", "프로모션", "세일", "할인")
        private val RETAIN_KEYWORDS = WORK_KEYWORDS + URGENT_KEYWORDS
        private val ACTION_KEYWORDS = listOf("확인", "수정", "제출", "보내", "공유", "참석", "준비", "처리")
        private val DEADLINE_PATTERNS = listOf("오늘까지", "내일까지", "이번 주", "마감", "deadline")
    }
}
