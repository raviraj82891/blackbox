package com.example.blackbox.data.repository

import android.content.Context
import android.content.ContextWrapper
import com.example.blackbox.data.api.AWSBackendApi
import com.example.blackbox.data.api.AuthRequest
import com.example.blackbox.data.api.AuthResponse
import com.example.blackbox.data.api.EmergencyContactDto
import com.example.blackbox.data.api.IncidentDownloadResponse
import com.example.blackbox.data.api.IncidentUploadRequest
import com.example.blackbox.data.api.IncidentUploadResponse
import com.example.blackbox.data.api.NotificationRequest
import com.example.blackbox.data.api.NotificationResponse
import com.example.blackbox.data.crypto.BufferAnchor
import com.example.blackbox.data.crypto.HashChainManager
import com.example.blackbox.data.crypto.KeyManagementService
import com.example.blackbox.data.crypto.MedicalIdData
import com.example.blackbox.data.db.EmergencyContact
import com.example.blackbox.data.db.EmergencyContactDao
import com.example.blackbox.data.db.IncidentReport
import com.example.blackbox.data.db.IncidentReportDao
import com.example.blackbox.data.db.SensorEvent
import com.example.blackbox.data.db.SensorEventDao
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class FactoryResetTest {

    private var sensorBufferCleared = false
    private var incidentsCleared = false
    private var contactsCleared = false
    private var preferencesPurged = false

    private class FakeSensorEventDao(val onClear: () -> Unit) : SensorEventDao {
        override suspend fun insert(event: SensorEvent): Long = 1L
        override suspend fun insertAll(events: List<SensorEvent>): List<Long> = events.map { 1L }
        override suspend fun getLastEvent(): SensorEvent? = null
        override fun getEventsSince(cutoffMs: Long) = emptyFlow<List<SensorEvent>>()
        override suspend fun getEventsSinceList(cutoffMs: Long) = emptyList<SensorEvent>()
        override suspend fun getAllEventsInWindow(startMs: Long, endMs: Long) = emptyList<SensorEvent>()
        override suspend fun getEventsOlderThanList(cutoffMs: Long) = emptyList<SensorEvent>()
        override suspend fun purgeOlderThan(cutoffMs: Long) = 0
        override fun getEventCount() = emptyFlow<Int>()
        override suspend fun getEventCountSync() = 0
        override suspend fun deleteAll() { onClear() }
    }

    private class FakeIncidentReportDao(val onClear: () -> Unit) : IncidentReportDao {
        override suspend fun insert(report: IncidentReport) {}
        override suspend fun update(report: IncidentReport) {}
        override fun getAllReports() = emptyFlow<List<IncidentReport>>()
        override suspend fun getAllReportsList() = emptyList<IncidentReport>()
        override suspend fun getReportById(id: String) = null
        override suspend fun deleteAll() { onClear() }
    }

    private class FakeEmergencyContactDao(val onClear: () -> Unit) : EmergencyContactDao {
        override suspend fun insertContact(contact: EmergencyContact) {}
        override suspend fun updateContact(contact: EmergencyContact) {}
        override suspend fun deleteContactById(id: String) {}
        override fun getAllContacts() = emptyFlow<List<EmergencyContact>>()
        override suspend fun getAllContactsList() = emptyList<EmergencyContact>()
        override fun getContactCount() = emptyFlow<Int>()
        override suspend fun getContactCountSync() = 0
        override suspend fun deleteAll() { onClear() }
    }

    private class FakeAWSBackendApi : AWSBackendApi {
        override suspend fun register(request: AuthRequest) = Response.success(AuthResponse("token", "user1"))
        override suspend fun login(request: AuthRequest) = Response.success(AuthResponse("token", "user1"))
        override suspend fun registerContact(contact: EmergencyContactDto) = Response.success(contact)
        override suspend fun uploadIncident(request: IncidentUploadRequest) = Response.success(IncidentUploadResponse("SUCCESS", request.incidentId, "msg", "s3key"))
        override suspend fun getIncident(id: String) = Response.success(IncidentDownloadResponse("id", 0L, "MANUAL", "bundle", "hash"))
        override suspend fun notifyContacts(id: String, request: NotificationRequest) = Response.success(NotificationResponse("SENT", id, 1))
    }

    private class TestKeyManagementService(
        val onPurge: () -> Unit
    ) : KeyManagementService(context = ContextWrapper(null)) {

        private var authToken: String? = null
        private var onboardingCompleted = false
        private var calibrationTimestamp = 0L
        private var cancelledCountdowns = 0
        private var safetyCheckIns = 0
        private var medicalId = MedicalIdData()
        private var bufferAnchor = BufferAnchor()

        override fun getAuthToken(): String? = authToken
        override fun saveAuthToken(token: String) { authToken = token }
        override fun clearAuthSession() { authToken = null }

        override fun isOnboardingCompleted(): Boolean = onboardingCompleted
        override fun setOnboardingCompleted(completed: Boolean) { onboardingCompleted = completed }

        override fun saveCalibrationResult(meanMagnitude: Double, threshold: Double, timestampMs: Long) {
            calibrationTimestamp = timestampMs
        }

        override fun getLastCalibrationSummary(): String? {
            if (calibrationTimestamp == 0L) return null
            return "Calibrated"
        }

        override fun getCancelledCountdownsCount(): Int = cancelledCountdowns
        override fun incrementCancelledCountdowns() { cancelledCountdowns++ }

        override fun getSafetyCheckInCount(): Int = safetyCheckIns
        override fun incrementSafetyCheckInCount() { safetyCheckIns++ }

        override fun getBufferAnchor(): BufferAnchor = bufferAnchor
        override fun saveBufferAnchor(anchor: BufferAnchor) { bufferAnchor = anchor }
        override fun clearBufferAnchor() { bufferAnchor = BufferAnchor() }

        override fun saveMedicalIdData(medicalId: MedicalIdData) { this.medicalId = medicalId }
        override fun getMedicalIdData(): MedicalIdData = medicalId

        override fun purgeAllEncryptedPreferencesAndKeys() {
            authToken = null
            onboardingCompleted = false
            calibrationTimestamp = 0L
            cancelledCountdowns = 0
            safetyCheckIns = 0
            medicalId = MedicalIdData()
            bufferAnchor = BufferAnchor()
            onPurge()
        }
    }

    private lateinit var keyManagementService: TestKeyManagementService
    private lateinit var repository: BlackboxRepository

    @Before
    fun setUp() {
        sensorBufferCleared = false
        incidentsCleared = false
        contactsCleared = false
        preferencesPurged = false

        keyManagementService = TestKeyManagementService { preferencesPurged = true }

        repository = BlackboxRepository(
            sensorEventDao = FakeSensorEventDao { sensorBufferCleared = true },
            incidentReportDao = FakeIncidentReportDao { incidentsCleared = true },
            emergencyContactDao = FakeEmergencyContactDao { contactsCleared = true },
            keyManagementService = keyManagementService,
            apiService = FakeAWSBackendApi()
        )
    }

    @Test
    fun testFactoryResetPurgesAllDatabaseTablesAndPreferences() = runBlocking {
        // 1. Populate non-default sensitive data into KeyManagementService
        keyManagementService.saveAuthToken("secret_token_123")
        keyManagementService.setOnboardingCompleted(true)
        keyManagementService.saveCalibrationResult(12.5, 28.0, System.currentTimeMillis())
        keyManagementService.incrementCancelledCountdowns()
        keyManagementService.incrementSafetyCheckInCount()
        keyManagementService.saveMedicalIdData(
            MedicalIdData(
                bloodGroup = "O+",
                allergies = "Penicillin",
                medicalNotes = "Diabetes",
                emergencyContactName = "Jane Doe",
                emergencyContactPhone = "+15551234567"
            )
        )
        keyManagementService.saveBufferAnchor(
            BufferAnchor(
                boundaryTimestampMs = 1700000000000L,
                boundaryEntryHash = "abc123hash",
                totalPurgedCount = 42L
            )
        )

        // Verify data was populated before reset
        assertEquals("secret_token_123", keyManagementService.getAuthToken())
        assertEquals(true, keyManagementService.isOnboardingCompleted())
        assertEquals(1, keyManagementService.getCancelledCountdownsCount())

        // 2. Perform Factory Reset
        repository.factoryResetAllData()

        // 3. Verify database tables are completely emptied
        assert(sensorBufferCleared) { "Sensor event buffer must be cleared" }
        assert(incidentsCleared) { "Incident reports history must be cleared" }
        assert(contactsCleared) { "Emergency contacts must be cleared" }
        assert(preferencesPurged) { "All encrypted preferences, keys, and tokens must be purged" }

        // 4. Verify all sensitive and identifiable state in KeyManagementService is purged
        assertNull("Auth token must be null after factory reset", keyManagementService.getAuthToken())
        assertFalse("Onboarding completion must be false after factory reset", keyManagementService.isOnboardingCompleted())
        assertNull("Calibration summary must be null after factory reset", keyManagementService.getLastCalibrationSummary())
        assertEquals("Cancelled countdowns count must be 0 after reset", 0, keyManagementService.getCancelledCountdownsCount())
        assertEquals("Safety check-ins count must be 0 after reset", 0, keyManagementService.getSafetyCheckInCount())
        assertEquals("Medical ID data must return default empty state", MedicalIdData(), keyManagementService.getMedicalIdData())
        assertEquals("Buffer anchor must reset to initial genesis hash", HashChainManager.INITIAL_HASH, keyManagementService.getBufferAnchor().boundaryEntryHash)
    }
}
