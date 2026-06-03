package com.example.openglow.data.rag

import android.util.Log
import androidx.room.withTransaction
import com.example.openglow.data.local.AppDatabase
import com.example.openglow.data.local.dao.NotificationDao
import com.example.openglow.data.local.dao.RagNotificationRow
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class RagSyncStatus(
    val total: Int,
    val uploaded: Int,
    val notUploaded: Int
)

data class RagSyncResult(
    val uploadedCount: Int,
    val failedCount: Int
)

data class RagConnectionTestResult(
    val success: Boolean,
    val message: String
)

private data class RagHttpResult(
    val statusCode: Int,
    val body: String
)

@Singleton
class RagRepository @Inject constructor(
    private val db: AppDatabase,
    private val notificationDao: NotificationDao
) {
    // Notification text is sensitive. Production deployments require HTTPS, user
    // authentication, server-side access control, masked logs, and deletion support.
    suspend fun getSyncStatus(): RagSyncStatus = withContext(Dispatchers.IO) {
        RagSyncStatus(
            total = notificationDao.getTotalNotificationCount(),
            uploaded = notificationDao.getRagUploadedNotificationCount(),
            notUploaded = notificationDao.getRagNotUploadedNotificationCount()
        )
    }

    fun getServerBaseUrl(): String = RagConfig.BASE_URL

    suspend fun testServerConnection(): RagConnectionTestResult = withContext(Dispatchers.IO) {
        runCatching {
            val rootStatusCode = getStatusCode("/")
            val ingestResult = postJsonForStatus(RagConfig.INGEST_PATH, createConnectionTestPayload())
            val queryResult = postJsonForStatus(RagConfig.QUERY_PATH, createConnectionTestPayload())

            when {
                rootStatusCode !in 200..399 -> {
                    RagConnectionTestResult(false, connectionFailureMessage("HTTP $rootStatusCode"))
                }

                ingestResult.statusCode == HttpURLConnection.HTTP_NOT_FOUND -> {
                    RagConnectionTestResult(
                        false,
                        "RAG server is reachable, but the ingest Flow endpoint is not. " +
                            "Check RagConfig.INGEST_FLOW_ID. HTTP ${ingestResult.statusCode}"
                    )
                }

                queryResult.statusCode == HttpURLConnection.HTTP_NOT_FOUND -> {
                    RagConnectionTestResult(
                        false,
                        "RAG server is reachable, but the query Flow endpoint is not. " +
                            "Check RagConfig.QUERY_FLOW_ID. HTTP ${queryResult.statusCode}"
                    )
                }

                isAuthFailure(ingestResult) || isAuthFailure(queryResult) -> {
                    RagConnectionTestResult(
                        false,
                        "RAG server and Flow endpoints are reachable, but Langflow requires an API key. " +
                            "Set RagConfig.API_KEY or run Langflow with auth disabled for local development."
                    )
                }

                else -> RagConnectionTestResult(true, "RAG server connection succeeded.")
            }
        }.getOrElse { error ->
            RagConnectionTestResult(false, connectionFailureMessage(error.message))
        }
    }

    suspend fun syncNotifications(userId: String = RagConfig.DEFAULT_USER_ID): RagSyncResult =
        withContext(Dispatchers.IO) {
            require(userId.isNotBlank()) { "User ID is empty." }

            val pending = notificationDao.getNotUploadedNotificationsForRag()
            var uploadedCount = 0
            var failedCount = 0
            var firstFailure: Throwable? = null

            pending.chunked(RagConfig.BATCH_SIZE).forEach { batch ->
                runCatching {
                    val response = postJson(
                        path = RagConfig.INGEST_PATH,
                        body = createLangflowRunPayload(createIngestPayload(userId, batch))
                    )
                    val indexedIds = response.optJSONArray("indexedIds")
                        ?.toStringList()
                        ?.toSet()
                    if (!response.optBoolean("success", true)) {
                        error("RAG server returned an upload failure.")
                    }

                    val uploadedRows = if (indexedIds == null) {
                        batch
                    } else {
                        batch.filter { documentId(userId, it.notificationId) in indexedIds }
                    }
                    val uploadedAt = System.currentTimeMillis()
                    db.withTransaction {
                        uploadedRows.forEach { row ->
                            notificationDao.markNotificationAsRagUploaded(
                                notificationId = row.notificationId,
                                uploadedAt = uploadedAt,
                                remoteDocumentId = documentId(userId, row.notificationId)
                            )
                        }
                    }
                    uploadedCount += uploadedRows.size
                    failedCount += batch.size - uploadedRows.size
                }.onFailure { error ->
                    if (firstFailure == null) {
                        firstFailure = error
                    }
                    failedCount += batch.size
                }
            }

            if (pending.isNotEmpty() && uploadedCount == 0 && failedCount > 0) {
                throw IllegalStateException(
                    firstFailure?.message
                        ?: "Could not communicate with the RAG server. Check the network and server URL.",
                    firstFailure
                )
            }

            RagSyncResult(uploadedCount = uploadedCount, failedCount = failedCount)
        }

    suspend fun askQuestion(
        question: String,
        userId: String = RagConfig.DEFAULT_USER_ID
    ): String = withContext(Dispatchers.IO) {
        require(userId.isNotBlank()) { "User ID is empty." }
        require(question.isNotBlank()) { "Enter a question." }

        val response = postJson(
            path = RagConfig.QUERY_PATH,
            body = createLangflowRunPayload(
                JSONObject()
                    .put("userId", userId)
                    .put("question", question.trim())
                    .put("topK", RagConfig.TOP_K)
            )
        )

        extractAnswer(response)
            ?: error("Could not find an answer in the RAG server response.")
    }

    private fun createLangflowRunPayload(input: JSONObject): JSONObject =
        JSONObject()
            .put("input_value", input.toString())
            .put("input_type", "chat")
            .put("output_type", "chat")

    private fun createConnectionTestPayload(): JSONObject =
        createLangflowRunPayload(JSONObject().put("ping", "connection_test"))

    private fun createIngestPayload(
        userId: String,
        rows: List<RagNotificationRow>
    ): JSONObject {
        val documents = JSONArray()
        rows.forEach { row ->
            documents.put(
                JSONObject()
                    .put("id", documentId(userId, row.notificationId))
                    .put("text", createDocumentText(row))
                    .put("metadata", createMetadata(userId, row))
            )
        }
        return JSONObject()
            .put("userId", userId)
            .put("documents", documents)
    }

    private fun createMetadata(userId: String, row: RagNotificationRow): JSONObject =
        JSONObject()
            .put("userId", userId)
            .put("source", "notification")
            .put("notificationId", row.notificationId)
            .put("senderId", row.senderId)
            .put("identifierId", row.identifierId)
            .put("senderName", row.senderName)
            .put("senderDisplayName", row.senderDisplayName)
            .put("platform", row.platform)
            .put("identifierValue", row.identifierValue)
            .put("packageName", row.packageName)
            .put("timestamp", ISO_FORMATTER.format(Instant.ofEpochMilli(row.timestamp)))
            .put("localTimestamp", row.timestamp)
            .put("isSummarized", row.isSummarized)

    private fun createDocumentText(row: RagNotificationRow): String {
        val time = LOCAL_FORMATTER.format(Instant.ofEpochMilli(row.timestamp))
        val sender = row.senderName ?: row.senderDisplayName
        return "[${platformLabel(row.platform)}] Notification from $sender at $time. Content: ${row.content}"
    }

    private fun extractAnswer(response: JSONObject): String? =
        listOf("answer", "result", "output", "message")
            .firstNotNullOfOrNull { key -> response.optString(key).takeIf(String::isNotBlank) }
            ?: response.optJSONArray("outputs")
                ?.optJSONObject(0)
                ?.optJSONArray("outputs")
                ?.optJSONObject(0)
                ?.optJSONObject("results")
                ?.optJSONObject("message")
                ?.optString("text")
                ?.takeIf(String::isNotBlank)

    private fun getStatusCode(path: String): Int {
        check(RagConfig.BASE_URL.isNotBlank()) { "RAG server URL is empty." }

        val connection = URL(RagConfig.BASE_URL.trimEnd('/') + path).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            connection.responseCode
        } finally {
            connection.disconnect()
        }
    }

    private fun isAuthFailure(result: RagHttpResult): Boolean =
        result.statusCode == HttpURLConnection.HTTP_UNAUTHORIZED ||
            result.statusCode == HttpURLConnection.HTTP_FORBIDDEN ||
            result.body.contains("API key", ignoreCase = true)

    private fun postJsonForStatus(path: String, body: JSONObject): RagHttpResult {
        check(RagConfig.BASE_URL.isNotBlank()) { "RAG server URL is empty." }

        val connection = URL(RagConfig.BASE_URL.trimEnd('/') + path).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 10_000
            connection.readTimeout = 30_000
            connection.doOutput = true
            applyJsonRequestHeaders(connection)
            connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body.toString()) }

            val responseBody = (if (connection.responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            })?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

            RagHttpResult(connection.responseCode, responseBody)
        } finally {
            connection.disconnect()
        }
    }

    private fun postJson(path: String, body: JSONObject): JSONObject {
        check(RagConfig.BASE_URL.isNotBlank()) { "RAG server URL is empty." }

        val connection = URL(RagConfig.BASE_URL.trimEnd('/') + path).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 10_000
            connection.readTimeout = 30_000
            connection.doOutput = true
            applyJsonRequestHeaders(connection)
            connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body.toString()) }

            val responseBody = (if (connection.responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            })?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

            if (connection.responseCode !in 200..299) {
                error(
                    "RAG server request failed. HTTP ${connection.responseCode}" +
                        responseBody.takeIf(String::isNotBlank)
                            ?.let { "\nResponse: ${it.take(MAX_ERROR_RESPONSE_LENGTH)}" }
                            .orEmpty()
                )
            }
            val response = runCatching { JSONObject(responseBody) }
                .getOrElse {
                    error(
                        "RAG server response was not valid JSON." +
                            responseBody.takeIf(String::isNotBlank)
                                ?.let { "\nResponse: ${it.take(MAX_ERROR_RESPONSE_LENGTH)}" }
                                .orEmpty()
                    )
                }
            Log.d(TAG, "[RAG] API statusCode=${connection.responseCode}")
            Log.d(TAG, "[RAG] API response keys=${response.keys().asSequence().toList()}")
            response
        } catch (e: Exception) {
            throw IllegalStateException(
                e.message ?: "Could not communicate with the RAG server. Check the network and server URL.",
                e
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun applyJsonRequestHeaders(connection: HttpURLConnection) {
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        if (RagConfig.API_KEY.isNotBlank()) {
            connection.setRequestProperty("x-api-key", RagConfig.API_KEY)
        }
    }

    private fun JSONArray.toStringList(): List<String> =
        (0 until length()).mapNotNull { index -> optString(index).takeIf(String::isNotBlank) }

    private fun documentId(userId: String, notificationId: Long): String =
        "${userId}_notification_$notificationId"

    private fun platformLabel(platform: String): String = when (platform) {
        "KAKAO" -> "KakaoTalk"
        "SMS" -> "SMS"
        "EMAIL" -> "Email"
        else -> platform
    }

    private fun connectionFailureMessage(detail: String?): String =
        buildString {
            append("Could not connect to the RAG server. Check the server URL, Wi-Fi, firewall, and Langflow run options.")
            if (!detail.isNullOrBlank()) {
                append("\n\nDetail: ").append(detail)
            }
            append(
                "\n\nChecklist:\n" +
                    "1. Make sure the PC and Android device are on the same Wi-Fi.\n" +
                    "2. Run Langflow with --host 0.0.0.0.\n" +
                    "3. Make sure the app server URL uses the PC IPv4 address.\n" +
                    "4. Allow port 7860 through Windows Firewall.\n" +
                    "5. Make sure AndroidManifest has INTERNET permission.\n" +
                    "6. Make sure cleartext HTTP traffic is allowed for this test build."
            )
        }

    companion object {
        private const val TAG = "RagRepository"
        private const val MAX_ERROR_RESPONSE_LENGTH = 500
        private val ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME
            .withZone(ZoneId.systemDefault())
        private val LOCAL_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault())
    }
}
