package com.example.openglaw

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationLogListenerService : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "알림 접근 서비스가 연결되었습니다. 대상 패키지: ${NotificationTargetPackages.targetPackages}")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)

        if (sbn.packageName !in NotificationTargetPackages.targetPackages) {
            return
        }

        val receivedNotification = buildReceivedNotification(sbn)
        logNotification(receivedNotification)
    }

    private fun buildReceivedNotification(sbn: StatusBarNotification): ReceivedNotification {
        val notification = sbn.notification
        val extras = notification.extras
        val extrasMap = NotificationValueFormatter.bundleToMap(extras)

        return ReceivedNotification(
            packageName = sbn.packageName,
            appName = findAppName(sbn.packageName),
            notificationKey = sbn.key.orEmpty(),
            postTime = sbn.postTime,
            postTimeText = DATE_FORMAT.format(Date(sbn.postTime)),
            title = extras.getTextOrNull(Notification.EXTRA_TITLE),
            text = extras.getTextOrNull(Notification.EXTRA_TEXT),
            subText = extras.getTextOrNull(Notification.EXTRA_SUB_TEXT),
            bigText = extras.getTextOrNull(Notification.EXTRA_BIG_TEXT),
            summaryText = extras.getTextOrNull(Notification.EXTRA_SUMMARY_TEXT),
            conversationTitle = extras.getTextOrNull("android.conversationTitle"),
            infoText = extras.getTextOrNull(Notification.EXTRA_INFO_TEXT),
            category = notification.category,
            extras = extrasMap,
        )
    }

    private fun logNotification(notification: ReceivedNotification) {
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

        Log.i(TAG, "pretty json:\n${notification.toJsonObject().toString(2)}")
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

        private val REQUESTED_EXTRA_KEYS = listOf(
            "android.title",
            "android.text",
            "android.subText",
            "android.bigText",
            "android.summaryText",
            "android.infoText",
            "android.conversationTitle",
            "android.messages",
            "android.template",
            "android.people",
            "android.picture",
        )
    }
}
