package com.example.openglow.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.openglow.data.repository.NotificationRepository
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

class AnalyzeNotificationWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val senderId = inputData.getLong(KEY_SENDER_ID, 0L)
        if (senderId <= 0L) return Result.failure()

        val entryPoint = EntryPoints.get(
            applicationContext,
            AnalyzeNotificationWorkerEntryPoint::class.java,
        )

        return entryPoint.notificationRepository()
            .generateSummaryIfNeeded(senderId)
            .fold(
                onSuccess = { Result.success() },
                onFailure = { Result.retry() },
            )
    }

    companion object {
        private const val KEY_NOTIFICATION_ID = "notificationId"
        private const val KEY_SENDER_ID = "senderId"

        fun enqueue(
            context: Context,
            notificationId: Long,
            senderId: Long,
        ) {
            val request = OneTimeWorkRequestBuilder<AnalyzeNotificationWorker>()
                .setInputData(
                    Data.Builder()
                        .putLong(KEY_NOTIFICATION_ID, notificationId)
                        .putLong(KEY_SENDER_ID, senderId)
                        .build(),
                )
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AnalyzeNotificationWorkerEntryPoint {
    fun notificationRepository(): NotificationRepository
}
