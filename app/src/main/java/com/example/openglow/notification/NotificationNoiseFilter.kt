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
    )

    fun shouldSkip(notification: ReceivedNotification): Boolean {
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
