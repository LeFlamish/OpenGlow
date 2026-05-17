package com.example.openglaw

import org.json.JSONObject

/**
 * 화면 표시나 저장이 아니라 로그 확인을 위한 임시 데이터 구조입니다.
 */
data class ReceivedNotification(
    val packageName: String,
    val appName: String,
    val notificationKey: String,
    val postTime: Long,
    val postTimeText: String,
    val title: String?,
    val text: String?,
    val subText: String?,
    val bigText: String?,
    val summaryText: String?,
    val conversationTitle: String?,
    val infoText: String?,
    val category: String?,
    val extras: Map<String, Any?>,
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("packageName", packageName)
        put("appName", appName)
        put("notificationKey", notificationKey)
        put("postTime", postTime)
        put("postTimeText", postTimeText)
        put("title", title)
        put("text", text)
        put("subText", subText)
        put("bigText", bigText)
        put("summaryText", summaryText)
        put("conversationTitle", conversationTitle)
        put("infoText", infoText)
        put("category", category)
        put("extras", NotificationValueFormatter.toJsonObject(extras))
    }
}
