package com.example.openglow

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.openglow.data.repository.NotificationRepository
import com.example.openglow.notification.NotificationTextExtractor
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NotificationLogListenerService : NotificationListenerService() {

    @Inject
    lateinit var repository: NotificationRepository

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "Notification listener connected. targets=${NotificationTargetPackages.targetPackages}")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)

        if (sbn.packageName !in NotificationTargetPackages.targetPackages) {
            return
        }

        val receivedNotification = buildReceivedNotification(sbn)
        logNotification(receivedNotification)

        serviceScope.launch {
            try {
                repository.handleNewNotification(receivedNotification)
                Log.d(TAG, "Notification passed to repository")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to handle notification: ${e.message}")
            }
        }
    }

    private fun buildReceivedNotification(sbn: StatusBarNotification): ReceivedNotification {
        val notification = sbn.notification
        val extras = notification.extras
        val extrasMap = NotificationValueFormatter.bundleToMap(extras)
        val extracted = NotificationTextExtractor.extract(sbn)

        return ReceivedNotification(
            packageName = sbn.packageName,
            appName = findAppName(sbn.packageName),
            notificationKey = sbn.key.orEmpty(),
            postTime = sbn.postTime,
            postTimeText = DATE_FORMAT.format(Date(sbn.postTime)),
            title = extracted.title ?: extras.getTextOrNull(Notification.EXTRA_TITLE),
            text = extras.getTextOrNull(Notification.EXTRA_TEXT),
            subText = extras.getTextOrNull(Notification.EXTRA_SUB_TEXT),
            bigText = extras.getTextOrNull(Notification.EXTRA_BIG_TEXT),
            summaryText = extras.getTextOrNull(Notification.EXTRA_SUMMARY_TEXT),
            conversationTitle = extracted.conversationTitle ?: extras.getTextOrNull("android.conversationTitle"),
            infoText = extras.getTextOrNull(Notification.EXTRA_INFO_TEXT),
            category = notification.category,
            extras = extrasMap,
            extractedFullText = extracted.fullText,
            textFragments = extracted.fragments,
            extractionSourceTypes = extracted.sourceTypes,
            completenessConfidence = extracted.completenessConfidence,
            isLikelyComplete = extracted.isLikelyComplete,
            groupHint = extracted.groupHint,
            extractionDebugInfo = extracted.debugInfo,
        )
    }

    private fun logNotification(notification: ReceivedNotification) {
        // Notification content is sensitive. Never print the full payload in production logs.
        // Log.i(
        //     TAG,
        //     "Notification received: packageName=${notification.packageName}, " +
        //         "postTime=${notification.postTime}, category=${notification.category}, " +
        //         "textLength=${notification.extractedFullText.length}, " +
        //         "confidence=${notification.completenessConfidence}",
        // )

        val importantExtras = REQUESTED_EXTRA_KEYS.joinToString(separator = "\n") { key ->
            "$key = ${NotificationValueFormatter.toReadableString(notification.extras[key])}"
        }

        val allExtras = notification.extras.entries.joinToString(separator = "\n") { (key, value) ->
            "$key = ${NotificationValueFormatter.toReadableString(value)}"
        }

        Log.i(
            TAG,
            """
            ================ 알림 수신 ================
            packageName: ${notification.packageName}
            appName: ${notification.appName}
            notificationKey: ${notification.notificationKey}
            postTime: ${notification.postTimeText} (${notification.postTime})
            category: ${notification.category}

            title: ${notification.title}
            text: ${notification.text}
            subText: ${notification.subText}
            bigText: ${notification.bigText}
            summaryText: ${notification.summaryText}
            conversationTitle: ${notification.conversationTitle}
            infoText: ${notification.infoText}

            주요 extras:
            $importantExtras

            전체 extras:
            $allExtras
            ==========================================
            """.trimIndent(),
        )

    }

    private fun findAppName(packageName: String): String {
        return runCatching {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        }.getOrElse { packageName }
    }

    private fun android.os.Bundle?.getTextOrNull(key: String): String? {
        return this?.get(key)?.toString()
    }

    companion object {
        private const val TAG = "NotificationListener"
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA)

        // 로그에서 먼저 확인하고 싶은 핵심 extras 키 목록.
        private val REQUESTED_EXTRA_KEYS = listOf(
            Notification.EXTRA_TITLE,
            Notification.EXTRA_TEXT,
            Notification.EXTRA_SUB_TEXT,
            Notification.EXTRA_BIG_TEXT,
            Notification.EXTRA_SUMMARY_TEXT,
            Notification.EXTRA_INFO_TEXT,
            Notification.EXTRA_TEXT_LINES,
            Notification.EXTRA_MESSAGES,
            "android.conversationTitle",
            "android.isGroupConversation",
            "android.template",
            "android.remoteInputHistory",
        )
    }
}
