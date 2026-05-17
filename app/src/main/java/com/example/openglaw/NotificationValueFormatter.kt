package com.example.openglaw

import android.os.Bundle
import android.os.Parcelable
import org.json.JSONArray
import org.json.JSONObject

/**
 * Notification extras 안에는 CharSequence, Bundle, Parcelable 배열 등 다양한 값이 들어옵니다.
 * 원본 구조를 최대한 잃지 않되, 로그와 JSON으로 확인 가능한 형태로 변환합니다.
 */
object NotificationValueFormatter {
    fun bundleToMap(bundle: Bundle?): Map<String, Any?> {
        if (bundle == null) return emptyMap()

        return bundle.keySet()
            .sorted()
            .associateWith { key -> normalizeValue(bundle.get(key)) }
    }

    fun toJsonObject(map: Map<String, Any?>): JSONObject = JSONObject().apply {
        map.forEach { (key, value) -> put(key, toJsonValue(value)) }
    }

    fun toReadableString(value: Any?): String = when (value) {
        null -> "null"
        is Map<*, *> -> value.entries.joinToString(
            prefix = "{",
            postfix = "}",
        ) { "${it.key}=${toReadableString(it.value)}" }
        is Iterable<*> -> value.joinToString(prefix = "[", postfix = "]") { toReadableString(it) }
        is Array<*> -> value.joinToString(prefix = "[", postfix = "]") { toReadableString(it) }
        else -> value.toString()
    }

    private fun normalizeValue(value: Any?): Any? = when (value) {
        null -> null
        is CharSequence -> value.toString()
        is Bundle -> bundleToMap(value)
        is Array<*> -> value.map { normalizeValue(it) }
        is Iterable<*> -> value.map { normalizeValue(it) }
        is Parcelable -> parcelableToReadableValue(value)
        else -> value
    }

    private fun parcelableToReadableValue(value: Parcelable): Any {
        return runCatching {
            val className = value.javaClass.name
            val text = value.toString()
            mapOf(
                "_class" to className,
                "_value" to text,
            )
        }.getOrElse { value.toString() }
    }

    private fun toJsonValue(value: Any?): Any = when (value) {
        null -> JSONObject.NULL
        is Map<*, *> -> JSONObject().apply {
            value.forEach { (mapKey, mapValue) ->
                put(mapKey?.toString().orEmpty(), toJsonValue(mapValue))
            }
        }
        is Iterable<*> -> JSONArray().apply {
            value.forEach { put(toJsonValue(it)) }
        }
        is Array<*> -> JSONArray().apply {
            value.forEach { put(toJsonValue(it)) }
        }
        is Number, is Boolean, is String -> value
        else -> value.toString()
    }
}
