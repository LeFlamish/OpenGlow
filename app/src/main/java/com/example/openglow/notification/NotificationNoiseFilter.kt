package com.example.openglow.notification

import com.example.openglow.ReceivedNotification

object NotificationNoiseFilter {
    private val unreadPatterns = listOf(
        Regex("^\\d+\\s+unread\\s+messages?$", RegexOption.IGNORE_CASE),
        Regex("^\\d+\\s+unread\\s+message\\(s\\)$", RegexOption.IGNORE_CASE),
        Regex("^\\d+\\s+new\\s+messages?$", RegexOption.IGNORE_CASE),
        Regex("^읽지 않은 메시지\\s*\\d+개$"),
        Regex("^안 읽은 메시지(\\s*\\d+개)?$"),
        Regex("^새 메시지\\s*\\d+개$"),
        Regex("^\\d+개의 새 메시지$"),
        Regex("^메시지\\s*\\d+개$"),
        // 카카오톡 집계 알림은 개수가 앞에 오는 형식이다: "54개의 안 읽은 메시지"
        Regex("^\\d+개의\\s*안\\s*읽은\\s*메시지$"),
        Regex("^\\d+개의\\s*읽지\\s*않은\\s*메시지$"),
    )

    fun shouldSkip(notification: ReceivedNotification): Boolean {
        // 실제 메시지 본문(MessagingStyle)이 있으면 노이즈가 아니므로 절대 거르지 않는다.
        val hasRealMessage = notification.extractionSourceTypes.contains("EXTRA_MESSAGES") ||
            notification.extras["android.messages"] != null

        // 사용자에게 실제로 보이는 주요 텍스트 필드.
        val primaryFields = listOfNotNull(
            notification.title,
            notification.text,
            notification.subText,
            notification.summaryText,
            notification.bigText,
        ).map { it.trim() }.filter { it.isNotBlank() }

        // "54개의 안 읽은 메시지" 같은 집계/요약 알림:
        // 실제 메시지가 없고, 보이는 텍스트가 전부 "안 읽은 메시지 개수" 문구면 저장하지 않는다.
        if (!hasRealMessage && primaryFields.isNotEmpty() && primaryFields.all { isUnreadCountText(it) }) {
            return true
        }

        // 기존 로직: 추출 텍스트/제목/조각이 비었거나 전부 숫자·카운트 문구뿐인 경우.
        val text = notification.extractedFullText.trim()
        val title = notification.title.orEmpty().trim()
        val fragments = notification.textFragments.map { it.trim() }.filter { it.isNotBlank() }
        val candidates = (listOf(text, title) + fragments).filter { it.isNotBlank() }

        if (candidates.isEmpty()) return true
        if (isUnreadCountText(text)) return true

        val hasOnlyCountLikeText = candidates.all { value ->
            isUnreadCountText(value) || value.matches(Regex("^\\d+$"))
        }
        return hasOnlyCountLikeText
    }

    private fun isUnreadCountText(value: String): Boolean {
        val normalized = value
            .replace(Regex("\\s+"), " ")
            .trim()
        return unreadPatterns.any { it.matches(normalized) }
    }
}
