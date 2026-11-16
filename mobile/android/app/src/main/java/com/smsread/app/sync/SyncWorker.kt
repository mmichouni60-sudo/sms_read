package com.smsread.app.sync

import android.content.Context
import androidx.work.*
import com.smsread.app.data.SmsDatabase

/**
 * Sync is intentionally disabled until authenticated HTTPS API client is implemented.
 * Never ACK or discard data without server confirmation.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val pending = SmsDatabase.get(applicationContext).events().pending()
        if (pending.isEmpty()) return Result.success()
        return Result.retry()
    }

    companion object {
        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, java.time.Duration.ofSeconds(30))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "sms-sync", ExistingWorkPolicy.KEEP, request
            )
        }
    }
}
