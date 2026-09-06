package com.example.blackbox.domain.pruning

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.blackbox.data.crypto.KeyManagementService
import com.example.blackbox.data.db.BlackboxDatabase
import com.example.blackbox.data.repository.BlackboxRepository
import com.example.blackbox.data.api.RetrofitClient

/**
 * Background WorkManager task enforcing the rolling buffer retention window (default 60 min).
 * Automatically purges expired sensor entries to satisfy the privacy guarantee:
 * "Rolling buffer, not a permanent log. Anything older than the retention window is continuously purged."
 */
class BufferPruningWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val kms = KeyManagementService(applicationContext)
            val dbPassphrase = kms.getOrCreateDatabasePassphrase()
            val db = BlackboxDatabase.getInstance(applicationContext, dbPassphrase)
            val repo = BlackboxRepository(db.sensorEventDao(), db.incidentReportDao(), kms, RetrofitClient.apiService)

            val purgedCount = repo.purgeExpiredBuffer(retentionMinutes = 60)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
