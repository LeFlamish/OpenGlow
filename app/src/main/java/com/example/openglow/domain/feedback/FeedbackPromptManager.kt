package com.example.openglow.domain.feedback

import android.content.Context
import com.example.openglow.data.entity.AnalysisLogEntity
import com.example.openglow.data.local.dao.AnalysisLogDao
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class FeedbackPromptManager @Inject constructor(
    @ApplicationContext context: Context,
    private val analysisLogDao: AnalysisLogDao,
) {
    private val prefs = context.getSharedPreferences("openglow_feedback", Context.MODE_PRIVATE)

    suspend fun pickCandidate(): AnalysisLogEntity? {
        if (!canAskToday()) return null

        val candidate = analysisLogDao.getPendingFeedbackCandidates(limit = 10)
            .firstOrNull { log ->
                log.confidence < 0.7f || log.importance in setOf("URGENT", "HIGH")
            }
            ?: return null

        analysisLogDao.markFeedbackRequested(listOf(candidate.id))
        prefs.edit()
            .putLong(KEY_LAST_ASKED_AT, System.currentTimeMillis())
            .apply()

        return candidate
    }

    private fun canAskToday(): Boolean {
        val lastAskedAt = prefs.getLong(KEY_LAST_ASKED_AT, 0L)
        return System.currentTimeMillis() - lastAskedAt >= ONE_DAY_MS
    }

    private companion object {
        private const val KEY_LAST_ASKED_AT = "lastAskedAt"
        private const val ONE_DAY_MS = 24L * 60L * 60L * 1000L
    }
}
