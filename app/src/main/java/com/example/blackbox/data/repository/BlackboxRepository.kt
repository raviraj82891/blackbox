package com.example.blackbox.data.repository

import com.example.blackbox.data.api.AWSBackendApi
import com.example.blackbox.data.api.IncidentUploadRequest
import com.example.blackbox.data.crypto.HashChainManager
import com.example.blackbox.data.crypto.KeyManagementService
import com.example.blackbox.data.db.EventType
import com.example.blackbox.data.db.IncidentReport
import com.example.blackbox.data.db.IncidentReportDao
import com.example.blackbox.data.db.SensorEvent
import com.example.blackbox.data.db.SensorEventDao
import com.example.blackbox.data.db.TriggerType
import com.example.blackbox.data.db.UploadStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class BlackboxRepository(
    private val sensorEventDao: SensorEventDao,
    private val incidentReportDao: IncidentReportDao,
    private val keyManagementService: KeyManagementService,
    private val apiService: AWSBackendApi
) {

    /**
     * Appends a new sensor reading to the Room buffer with SHA-256 hash chaining.
     */
    suspend fun recordSensorEvent(type: EventType, payloadJson: String, timestampMs: Long = System.currentTimeMillis()): Long = withContext(Dispatchers.IO) {
        val lastEvent = sensorEventDao.getLastEvent()
        val prevHash = lastEvent?.entryHash ?: HashChainManager.INITIAL_HASH
        val entryHash = HashChainManager.computeEntryHash(prevHash, payloadJson, timestampMs)

        val event = SensorEvent(
            timestampMs = timestampMs,
            type = type,
            payloadJson = payloadJson,
            prevHash = prevHash,
            entryHash = entryHash
        )
        sensorEventDao.insert(event)
    }

    /**
     * Observes sensor events recorded within the given window (default: last 60 minutes).
     */
    fun getRollingBufferEvents(windowMinutes: Long = 60): Flow<List<SensorEvent>> {
        val cutoff = System.currentTimeMillis() - (windowMinutes * 60 * 1000)
        return sensorEventDao.getEventsSince(cutoff)
    }

    fun getBufferEventCount(): Flow<Int> = sensorEventDao.getEventCount()

    /**
     * Purges sensor events older than the retention window (e.g. 60 minutes).
     */
    suspend fun purgeExpiredBuffer(retentionMinutes: Long = 60): Int = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - (retentionMinutes * 60 * 1000)
        sensorEventDao.purgeOlderThan(cutoff)
    }

    /**
     * Performs a complete one-tap data wipe, deleting all sensor events and incident reports.
     */
    suspend fun wipeAllData() = withContext(Dispatchers.IO) {
        sensorEventDao.deleteAll()
        incidentReportDao.deleteAll()
    }

    /**
     * Verifies cryptographic hash chain integrity of the rolling buffer.
     */
    suspend fun verifyBufferIntegrity(windowMinutes: Long = 60): Boolean = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - (windowMinutes * 60 * 1000)
        val events = sensorEventDao.getEventsSinceList(cutoff)
        HashChainManager.verifyChainIntegrity(events)
    }

    /**
     * Freezes current rolling buffer, reconstructs the timeline, generates a Merkle-style root hash,
     * encrypts the report client-side using AES-256-GCM, and stores an IncidentReport.
     */
    suspend fun freezeBufferAndCreateIncident(
        triggerType: TriggerType,
        windowMinutes: Long = 60,
        timelineJson: String
    ): IncidentReport = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - (windowMinutes * 60 * 1000)
        val events = sensorEventDao.getEventsSinceList(cutoff)

        val hashes = events.map { it.entryHash }
        val chainRootHash = HashChainManager.computeChainRootHash(hashes)

        // Envelope encryption client-side (Zero-Knowledge)
        val (encryptedBundle, keyBase64) = keyManagementService.encryptIncidentBundle(timelineJson)

        val report = IncidentReport(
            id = UUID.randomUUID().toString(),
            triggeredAt = System.currentTimeMillis(),
            triggerType = triggerType,
            timelineJson = timelineJson,
            chainRootHash = chainRootHash,
            uploadStatus = UploadStatus.ENCRYPTED,
            encryptedBundle = encryptedBundle,
            decryptionKey = keyBase64
        )

        incidentReportDao.insert(report)
        report
    }

    /**
     * Uploads encrypted incident bundle to AWS serverless backend.
     */
    suspend fun uploadIncidentToBackend(report: IncidentReport, contactIds: List<String>): UploadStatus = withContext(Dispatchers.IO) {
        if (report.encryptedBundle == null) return@withContext UploadStatus.FAILED

        val request = IncidentUploadRequest(
            incidentId = report.id,
            triggeredAt = report.triggeredAt,
            triggerType = report.triggerType.name,
            encryptedBundle = report.encryptedBundle,
            chainRootHash = report.chainRootHash,
            contactsToNotify = contactIds
        )

        try {
            val response = apiService.uploadIncident(request)
            if (response.isSuccessful) {
                val updated = report.copy(uploadStatus = UploadStatus.UPLOADED)
                incidentReportDao.update(updated)
                UploadStatus.UPLOADED
            } else {
                val updated = report.copy(uploadStatus = UploadStatus.FAILED)
                incidentReportDao.update(updated)
                UploadStatus.FAILED
            }
        } catch (e: Exception) {
            val updated = report.copy(uploadStatus = UploadStatus.FAILED)
            incidentReportDao.update(updated)
            UploadStatus.FAILED
        }
    }

    fun getAllIncidentReports(): Flow<List<IncidentReport>> = incidentReportDao.getAllReports()
}
