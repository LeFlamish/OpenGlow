package com.example.openglow.data.rag

import android.util.Log
import androidx.room.withTransaction
import com.example.openglow.data.local.AppDatabase
import com.example.openglow.data.local.dao.NoteDao
import com.example.openglow.data.local.dao.RagNoteRow
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
    private val noteDao: NoteDao
) {
    // Notification text is sensitive. Production deployments require HTTPS, user
    // authentication, server-side access control, masked logs, and deletion support.
    suspend fun getSyncStatus(): RagSyncStatus = withContext(Dispatchers.IO) {
        RagSyncStatus(
            total = noteDao.getTotalNoteCount(),
            uploaded = noteDao.getRagUploadedNoteCount(),
            notUploaded = noteDao.getRagNotUploadedNoteCount()
        )
    }

    fun getSyncStatusStream(): Flow<RagSyncStatus> =
        noteDao.getAllNotesFlow().map { notes ->
            val uploaded = notes.count { note ->
                note.ragUploadedAt != null &&
                    note.ragLastUploadedNoteUpdatedAt != null &&
                    note.ragLastUploadedNoteUpdatedAt >= note.updatedAt
            }
            RagSyncStatus(
                total = notes.size,
                uploaded = uploaded,
                notUploaded = notes.size - uploaded,
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

            val pending = noteDao.getNotUploadedNotesForRag()
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
                        batch.filter { documentId(userId, it.noteId) in indexedIds }
                    }
                    val uploadedAt = System.currentTimeMillis()
                    db.withTransaction {
                        uploadedRows.forEach { row ->
                            noteDao.markNoteAsRagUploaded(
                                noteId = row.noteId,
                                uploadedAt = uploadedAt,
                                remoteDocumentId = documentId(userId, row.noteId),
                                noteUpdatedAt = row.updatedAt,
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
        rows: List<RagNoteRow>
    ): JSONObject {
        val documents = JSONArray()
        rows.forEach { row ->
            documents.put(
                JSONObject()
                    .put("id", documentId(userId, row.noteId))
                    .put("text", createDocumentText(row))
                    .put("metadata", createMetadata(userId, row))
            )
        }
        return JSONObject()
            .put("userId", userId)
            .put("documents", documents)
    }

    private fun createMetadata(userId: String, row: RagNoteRow): JSONObject =
        JSONObject()
            .put("userId", userId)
            .put("source", "note")
            .put("noteId", row.noteId)
            .put("senderId", row.senderId)
            .put("senderDisplayName", row.senderDisplayName)
            .put("senderScope", row.senderScope)
            .put("platform", row.platform)
            .put("title", row.title)
            .put("latestImportance", row.latestImportance)
            .put("aggregateImportance", row.aggregateImportance)
            .put("latestIsWorkRelated", row.latestIsWorkRelated)
            .put("aggregateIsWorkRelated", row.aggregateIsWorkRelated)
            .put("latestMeetingDetected", row.latestMeetingDetected)
            .put("latestProjectDetected", row.latestProjectDetected)
            .put("latestDeadlineText", row.latestDeadlineText)
            .put("calendarCandidate", row.calendarCandidate)
            .put("notificationCount", row.notificationCount)
            .put("modelSource", row.modelSource)
            .put("confidence", row.confidence)
            .put("createdAt", ISO_FORMATTER.format(Instant.ofEpochMilli(row.createdAt)))
            .put("updatedAt", ISO_FORMATTER.format(Instant.ofEpochMilli(row.updatedAt)))
            .put("localCreatedAt", row.createdAt)
            .put("localUpdatedAt", row.updatedAt)

    private fun createDocumentText(row: RagNoteRow): String {
        val updatedTime = LOCAL_FORMATTER.format(Instant.ofEpochMilli(row.updatedAt))
        val retainedFacts = parseJsonArray(row.retainedFactsJson)
        val actionItems = parseJsonArray(row.latestActionItemsJson)
        return buildString {
            appendLine("Title: ${row.title}")
            appendLine("Sender: ${row.senderDisplayName} (${row.senderScope}, ${platformLabel(row.platform)})")
            appendLine("Updated: $updatedTime")
            appendLine("Importance: latest=${row.latestImportance}, aggregate=${row.aggregateImportance}")
            appendLine("Work related: latest=${row.latestIsWorkRelated}, aggregate=${row.aggregateIsWorkRelated}")
            row.latestDeadlineText?.takeIf(String::isNotBlank)?.let { appendLine("Deadline: $it") }
            appendLine("Latest summary: ${row.latestOneLineSummary}")
            appendLine("Final summary: ${row.finalSummary}")
            if (retainedFacts.isNotEmpty()) {
                appendLine("Retained facts:")
                retainedFacts.forEach { appendLine("- $it") }
            }
            if (actionItems.isNotEmpty()) {
                appendLine("Action items:")
                actionItems.forEach { appendLine("- $it") }
            }
            appendLine("Notification count summarized into this note: ${row.notificationCount}")
            appendLine("Model source: ${row.modelSource}, confidence=${row.confidence}")
        }.trim()
    }

    private fun extractAnswer(response: JSONObject): String? {
        val directAnswer = response.optString("answer").takeIf(String::isNotBlank)
        if (directAnswer != null) return extractAnswerFromPossibleJson(directAnswer)

        val candidates = listOfNotNull(
            response.optString("result").takeIf(String::isNotBlank),
            response.optString("output").takeIf(String::isNotBlank),
            response.optString("message").takeIf(String::isNotBlank),
            response.optJSONArray("outputs")
                ?.optJSONObject(0)
                ?.optJSONArray("outputs")
                ?.optJSONObject(0)
                ?.optJSONObject("results")
                ?.optJSONObject("message")
                ?.optString("text")
                ?.takeIf(String::isNotBlank),
        )

        return candidates.firstNotNullOfOrNull(::extractAnswerFromPossibleJson)
    }

    private fun extractAnswerFromPossibleJson(value: String): String? {
        val trimmed = value.trim()
        if (!trimmed.startsWith("{")) return trimmed

        return runCatching { JSONObject(trimmed) }
            .getOrNull()
            ?.let { json ->
                json.optString("answer").takeIf(String::isNotBlank)
                    ?: json.optString("result").takeIf(String::isNotBlank)
                    ?: json.optString("output").takeIf(String::isNotBlank)
                    ?: json.optString("message").takeIf(String::isNotBlank)
            }
            ?.trim()
            ?.takeIf(String::isNotBlank)
    }

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

    private fun parseJsonArray(json: String): List<String> =
        runCatching {
            val array = JSONArray(json)
            List(array.length()) { index -> array.optString(index) }
                .filter { it.isNotBlank() }
        }.getOrDefault(emptyList())

    private fun documentId(userId: String, noteId: Long): String =
        "${userId}_note_$noteId"

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

