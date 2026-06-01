package com.example.openglow.domain.classifier

import java.util.Locale
import javax.inject.Inject

class RuleBasedTextClassifier @Inject constructor() : TextClassifierClient {
    override suspend fun isAvailable(): Boolean = true

    override suspend fun classify(text: String): ClassificationHint {
        val normalized = text.lowercase(Locale.KOREA)
        val urgent = URGENT_KEYWORDS.any { normalized.contains(it) }
        val workScore = score(normalized, WORK_KEYWORDS)
        val meetingScore = score(normalized, MEETING_KEYWORDS)
        val projectScore = score(normalized, PROJECT_KEYWORDS)
        val lowValue = LOW_VALUE_KEYWORDS.any { normalized.contains(it) }

        val importance = when {
            urgent -> "URGENT"
            workScore >= 0.45f || meetingScore >= 0.45f || projectScore >= 0.45f -> "HIGH"
            lowValue -> "LOW"
            else -> "NORMAL"
        }

        val confidence = maxOf(workScore, meetingScore, projectScore)
            .coerceAtLeast(if (urgent || lowValue) 0.72f else 0.52f)
            .coerceAtMost(0.86f)

        return ClassificationHint(
            workRelatedScore = maxOf(workScore, meetingScore, projectScore),
            importanceHint = importance,
            meetingScore = meetingScore,
            projectScore = projectScore,
            confidence = confidence,
            modelName = "RuleBasedTextClassifier",
        )
    }

    private fun score(text: String, keywords: List<String>): Float {
        val matches = keywords.count { text.contains(it) }
        return (matches / 3f).coerceIn(0f, 1f)
    }

    private companion object {
        private val URGENT_KEYWORDS = listOf("긴급", "asap", "즉시", "오늘까지", "내일까지", "마감", "deadline")
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
            "일정",
            "메일",
            "업무",
            "코드",
            "테스트",
            "배포",
            "교수",
            "팀장",
            "사수",
        )
        private val MEETING_KEYWORDS = listOf("회의", "미팅", "zoom", "줌", "참석", "장소", "시간", "변경")
        private val PROJECT_KEYWORDS = listOf("프로젝트", "팀플", "개발", "코드", "보고서", "테스트", "배포", "담당")
        private val LOW_VALUE_KEYWORDS = listOf("광고", "쿠폰", "혜택", "프로모션", "세일", "할인")
    }
}
