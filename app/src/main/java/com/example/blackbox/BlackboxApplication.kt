package com.example.blackbox

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.blackbox.domain.pruning.BufferPruningWorker
import com.example.blackbox.domain.verification.HashChainVerificationWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class BlackboxApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        scheduleBackgroundWorkers()
    }

    private fun scheduleBackgroundWorkers() {
        try {
            val workManager = WorkManager.getInstance(this)

            // 1. Periodic 60m Rolling Buffer Pruner (Runs every 1 hour)
            val pruningRequest = PeriodicWorkRequestBuilder<BufferPruningWorker>(1, TimeUnit.HOURS)
                .build()
            workManager.enqueueUniquePeriodicWork(
                "TRACE_BufferPruningWork",
                ExistingPeriodicWorkPolicy.KEEP,
                pruningRequest
            )

            // 2. Periodic Hash-Chain Integrity Verification (Runs every 15 minutes)
            val verificationRequest = PeriodicWorkRequestBuilder<HashChainVerificationWorker>(15, TimeUnit.MINUTES)
                .build()
            workManager.enqueueUniquePeriodicWork(
                "TRACE_HashVerificationWork",
                ExistingPeriodicWorkPolicy.KEEP,
                verificationRequest
            )
        } catch (e: Exception) {
            // Background scheduling fallback
        }
    }
}
