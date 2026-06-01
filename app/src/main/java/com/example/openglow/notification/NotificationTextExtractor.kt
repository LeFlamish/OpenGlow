package com.example.openglow.notification

import android.app.Notification
import android.app.Person
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.service.notification.StatusBarNotification
import java.util.Locale
import kotlin.math.min

data class ExtractedNotificationText(
    val title: String?,
    val senderName: String?,
    val conversationTitle: String?,
    val fullText: String,
    val fragments: List<String>,
    val sourceTypes: List<String>,
    val isLikelyComplete: Boolean,
    val completenessConfidence: Float,
    val groupHint: Boolean?,
    val debugInfo: String,
)

object NotificationTextExtractor {
    private const val KEY_CONVERSATION_TITLE = "android.conversationTitle"
    private const val KEY_IS_GROUP_CONVERSATION = "android.isGroupConversation"
    private const val KEY_MESSAGES = "android.messages"
    private const val KEY_TEXT_LINES = "android.textLines"
    private const val KEY_REMOTE_INPUT_HISTORY = "android.remoteInputHistory"

    private val priorityKeys = listOf(
        Notification.EXTRA_BIG_TEXT to "EXTRA_BIG_TEXT",
        Notification.EXTRA_TEXT_LINES to "EXTRA_TEXT_LINES",
        Notification.EXTRA_TEXT to "EXTRA_TEXT",
        Notification.EXTRA_SUB_TEXT to "EXTRA_SUB_TEXT",
        Notification.EXTRA_SUMMARY_TEXT to "EXTRA_SUMMARY_TEXT",
        Notification.EXTRA_INFO_TEXT to "EXTRA_INFO_TEXT",
        KEY_REMOTE_INPUT_HISTORY to "REMOTE_INPUT_HISTORY",
        Notification.EXTRA_TITLE to "EXTRA_TITLE",
    )

    fun extract(sbn: StatusBarNotification): ExtractedNotificationText {
        val notification = sbn.notification
        val extras = notification.extras ?: Bundle.EMPTY
        val title = extras.textOrNull(Notification.EXTRA_TITLE)
        val conversationTitle = extras.textOrNull(KEY_CONVERSATION_TITLE)
        val groupHint = extras.booleanOrNull(KEY_IS_GROUP_CONVERSATION)

        val fragments = LinkedHashSet<String>()
        val sourceTypes = LinkedHashSet<String>()
        val messageSenders = LinkedHashSet<String>()

        val messageLines = extractMessageLines(extras, messageSenders)
        if (messageLines.isNotEmpty()) {
            sourceTypes += "EXTRA_MESSAGES"
            fragments += messageLines
        }

        priorityKeys.forEach { (key, sourceType) ->
            val values = extractKnownValue(extras, key)
            if (values.isNotEmpty()) {
                sourceTypes += sourceType
                fragments += values
            }
        }

        extras.keySet()
            .sorted()
            .filterNot { key ->
                key == KEY_MESSAGES ||
                    key == Notification.EXTRA_MESSAGES ||
                    priorityKeys.any { it.first == key }
            }
            .forEach { key ->
                val values = extractGenericText(extras.get(key))
                if (values.isNotEmpty()) {
                    sourceTypes += "EXTRA:$key"
                    fragments += values
                }
            }

        val cleanedFragments = fragments
            .mapNotNull { it.cleanText() }
            .dedupeByNormalizedText()

        val fullText = cleanedFragments.joinToString(separator = "\n")
        val confidence = estimateConfidence(
            packageName = sbn.packageName,
            sourceTypes = sourceTypes,
            fullText = fullText,
            hasConversationTitle = !conversationTitle.isNullOrBlank(),
        )

        val senderName = messageSenders.firstOrNull()
            ?: title
            ?: conversationTitle

        return ExtractedNotificationText(
            title = title,
            senderName = senderName,
            conversationTitle = conversationTitle,
            fullText = fullText,
            fragments = cleanedFragments,
            sourceTypes = sourceTypes.toList(),
            isLikelyComplete = confidence >= 0.7f,
            completenessConfidence = confidence,
            groupHint = groupHint,
            debugInfo = "length=${fullText.length}, sources=${sourceTypes.size}, confidence=$confidence",
        )
    }

    private fun extractKnownValue(extras: Bundle, key: String): List<String> {
        return when (val value = extras.get(key)) {
            is CharSequence -> listOf(value.toString())
            is String -> listOf(value)
            is Array<*> -> value.flatMap { extractGenericText(it) }
            is Iterable<*> -> value.flatMap { extractGenericText(it) }
            else -> extractGenericText(value)
        }
    }

    @Suppress("DEPRECATION")
    private fun extractMessageLines(
        extras: Bundle,
        senders: MutableSet<String>,
    ): List<String> {
        val array = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
            ?: extras.getParcelableArray(KEY_MESSAGES)
            ?: return emptyList()

        return array.mapNotNull { parcelable ->
            val messageBundle = parcelable as? Bundle ?: return@mapNotNull null
            val text = messageBundle.textOrNull("text")
                ?: messageBundle.textOrNull("android.text")
                ?: return@mapNotNull null
            val sender = extractMessageSender(messageBundle)
            if (!sender.isNullOrBlank()) {
                senders += sender
            }

            if (sender.isNullOrBlank()) text else "$sender: $text"
        }
    }

    private fun extractMessageSender(messageBundle: Bundle): String? {
        val senderFromPerson = messageBundle.get("sender_person")
            ?.let(::extractPersonName)
            ?: messageBundle.get("android.senderPerson")?.let(::extractPersonName)

        return senderFromPerson
            ?: messageBundle.textOrNull("sender")
            ?: messageBundle.textOrNull("android.sender")
            ?: messageBundle.textOrNull("person")
    }

    private fun extractPersonName(value: Any): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && value is Person) {
            return value.name?.toString()
        }

        return value.toString().takeIf { it.isNotBlank() }
    }

    private fun extractGenericText(value: Any?): List<String> {
        return when (value) {
            null -> emptyList()
            is CharSequence -> listOf(value.toString())
            is String -> listOf(value)
            is Bundle -> value.keySet().sorted().flatMap { extractGenericText(value.get(it)) }
            is Array<*> -> value.flatMap { extractGenericText(it) }
            is Iterable<*> -> value.flatMap { extractGenericText(it) }
            is Parcelable -> extractTextFromParcelable(value)
            else -> emptyList()
        }
    }

    private fun extractTextFromParcelable(value: Parcelable): List<String> {
        if (value is Bundle) return extractGenericText(value)

        val asString = value.toString()
        return if (looksLikeUsefulText(asString)) listOf(asString) else emptyList()
    }

    private fun Bundle.textOrNull(key: String): String? {
        return when (val value = get(key)) {
            is CharSequence -> value.toString()
            is String -> value
            else -> null
        }?.cleanText()
    }

    private fun Bundle.booleanOrNull(key: String): Boolean? {
        return when (val value = get(key)) {
            is Boolean -> value
            is String -> value.equals("true", ignoreCase = true)
            else -> null
        }
    }

    private fun estimateConfidence(
        packageName: String,
        sourceTypes: Set<String>,
        fullText: String,
        hasConversationTitle: Boolean,
    ): Float {
        if (fullText.isBlank()) return 0f

        var confidence = when {
            "EXTRA_MESSAGES" in sourceTypes -> 0.92f
            "EXTRA_BIG_TEXT" in sourceTypes -> 0.84f
            "EXTRA_TEXT_LINES" in sourceTypes -> 0.78f
            "EXTRA_TEXT" in sourceTypes -> 0.58f
            else -> 0.45f
        }

        if (hasConversationTitle) confidence += 0.04f
        if (fullText.length > 240) confidence += 0.08f
        if (isEmailPackage(packageName) && "EXTRA_MESSAGES" !in sourceTypes && "EXTRA_BIG_TEXT" !in sourceTypes) {
            confidence = min(confidence, 0.52f)
        }

        return confidence.coerceIn(0f, 0.98f)
    }

    private fun isEmailPackage(packageName: String): Boolean {
        return packageName in setOf(
            "com.google.android.gm",
            "com.samsung.android.email.provider",
            "com.microsoft.office.outlook",
        )
    }

    private fun String.cleanText(): String? {
        val cleaned = replace(Regex("\\s+"), " ")
            .trim()
        return cleaned.takeIf { it.isNotBlank() && looksLikeUsefulText(it) }
    }

    private fun looksLikeUsefulText(value: String): Boolean {
        val lowered = value.lowercase(Locale.US)
        if (lowered.startsWith("android.app.") || lowered.startsWith("android.os.")) return false
        if (lowered.startsWith("bundle[")) return false
        return value.any { it.isLetterOrDigit() }
    }

    private fun List<String>.dedupeByNormalizedText(): List<String> {
        val seen = LinkedHashSet<String>()
        return filter { text ->
            val key = text.lowercase(Locale.US)
            seen.add(key)
        }
    }
}
