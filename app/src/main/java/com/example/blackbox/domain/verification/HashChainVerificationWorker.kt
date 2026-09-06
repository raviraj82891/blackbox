package com.example.blackbox.domain.verification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.blackbox.data.repository.BlackboxRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Background WorkManager task that periodically verifies hash-chain integrity
 * independently of UI or ViewModel lifecycle.
 */
@HiltWorker
class HashChainVerificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: BlackboxRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val isIntact = repository.verifyBufferIntegrity(windowMinutes = 60)
            if (isIntact) Result.success() else Result.failure()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
