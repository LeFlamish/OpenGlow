package com.example.openglow.domain.training

import android.content.Context
import com.example.openglow.data.local.dao.AnalysisLogDao
import com.example.openglow.data.local.dao.FeedbackDao
import com.example.openglow.data.local.dao.NoteDao
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import org.json.JSONArray
import org.json.JSONObject

class TrainingDataExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val feedbackDao: FeedbackDao,
    private val analysisLogDao: AnalysisLogDao,
    private val noteDao: NoteDao,
) {
    suspend fun exportJsonl(
        includeTextPreview: Boolean,
        outputFile: File = defaultOutputFile(),
    ): Result<File> = runCatching {
        outputFile.parentFile?.mkdirs()

        val lines = feedbackDao.getAllFeedback().mapNotNull { feedback ->
            val log = analysisLogDao.getAnalysisLogById(feedback.analysisLogId) ?: return@mapNotNull null
            val note = noteDao.getNoteById(feedback.noteId)
            val input = JSONObject().apply {
                put("senderDisplayName", note?.senderDisplayName ?: JSONObject.NULL)
                put("senderScope", note?.senderScope ?: log.senderScope)
                put("platform", note?.platform ?: JSONObject.NULL)
                put("inputTextHash", log.inputTextHash)
                if (includeTextPreview) {
                    put("text", log.inputTextPreview)
                } else {
                    put("previewKeywords", JSONArray(extractKeywords(log.inputTextPreview)))
                }
            }
            val label = JSONObject().apply {
                put("importance", feedback.correctedImportance ?: log.importance)
                put("isWorkRelated", feedback.correctedIsWorkRelated ?: log.isWorkRelated)
                put("senderScope", feedback.correctedSenderScope ?: log.senderScope)
            }
            JSONObject()
                .put("input", input)
                .put("label", label)
                .toString()
        }

        outputFile.writeText(lines.joinToString(separator = "\n"), Charsets.UTF_8)
        outputFile
    }

    private fun defaultOutputFile(): File {
        return File(context.getExternalFilesDir(null), "openglow-training-feedback.jsonl")
    }

    private fun extractKeywords(text: String): List<String> {
        return text.split(Regex("[\\s,.;:/]+"))
            .map { it.trim() }
            .filter { it.length >= 2 }
            .distinct()
            .take(20)
    }
}
