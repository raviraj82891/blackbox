package com.example.blackbox.domain.trigger

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
import com.example.blackbox.data.crypto.KeyManagementService
import com.example.blackbox.data.db.EmergencyContact
import com.example.blackbox.data.db.EmergencyContactDao
import com.example.blackbox.data.db.EventType
import com.example.blackbox.data.db.IncidentReport
import com.example.blackbox.data.db.IncidentReportDao
import com.example.blackbox.data.db.SensorEvent
import com.example.blackbox.data.db.SensorEventDao
import com.example.blackbox.data.db.TriggerType
import com.example.blackbox.data.db.UploadStatus
import com.example.blackbox.data.repository.BlackboxRepository
import com.example.blackbox.service.ProtectionState
import com.example.blackbox.service.ProtectionStateManager
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.net.UnknownHostException

class SafetyLifecycleEndToEndTest {

    private val sensorEvents = mutableListOf<SensorEvent>()
    private val incidentReports = mutableListOf<IncidentReport>()
    private val emergencyContacts = mutableListOf<EmergencyContact>()

    private var isNetworkAvailable = true
    private var isNotificationSuccessful = true

    private class TestSensorDao(val events: MutableList<SensorEvent>) : SensorEventDao {
        override suspend fun insert(event: SensorEvent): Long {
            events.add(event)
            return events.size.toLong()
        }
        override suspend fun insertAll(eventsToInsert: List<SensorEvent>): List<Long> {
            events.addAll(eventsToInsert)
            return eventsToInsert.map { events.size.toLong() }
        }
        override suspend fun getLastEvent(): SensorEvent? = events.lastOrNull()
        override fun getEventsSince(cutoffMs: Long) = emptyFlow<List<SensorEvent>>()
        override suspend fun getEventsSinceList(cutoffMs: Long) = events.filter { it.timestampMs >= cutoffMs }
        override suspend fun getAllEventsInWindow(startMs: Long, endMs: Long) = events.filter { it.timestampMs in startMs..endMs }
        override suspend fun getEventsOlderThanList(cutoffMs: Long) = events.filter { it.timestampMs < cutoffMs }
        override suspend fun purgeOlderThan(cutoffMs: Long): Int {
            val countBefore = events.size
            events.removeAll { it.timestampMs < cutoffMs }
            return countBefore - events.size
        }
        override fun getEventCount() = emptyFlow<Int>()
        override suspend fun getEventCountSync() = events.size
        override suspend fun deleteAll() { events.clear() }
    }

    private class TestIncidentDao(val reports: MutableList<IncidentReport>) : IncidentReportDao {
        override suspend fun insert(report: IncidentReport) { reports.add(report) }
        override suspend fun update(report: IncidentReport) {
            val idx = reports.indexOfFirst { it.id == report.id }
            if (idx != -1) reports[idx] = report else reports.add(report)
        }
        override fun getAllReports() = emptyFlow<List<IncidentReport>>()
        override suspend fun getAllReportsList() = reports.toList()
        override suspend fun getReportById(id: String) = reports.firstOrNull { it.id == id }
        override suspend fun deleteAll() { reports.clear() }
    }

    private class TestContactDao(val contacts: MutableList<EmergencyContact>) : EmergencyContactDao {
        override suspend fun insertContact(contact: EmergencyContact) { contacts.add(contact) }
        override suspend fun updateContact(contact: EmergencyContact) {
            val idx = contacts.indexOfFirst { it.id == contact.id }
            if (idx != -1) contacts[idx] = contact
        }
        override suspend fun deleteContactById(id: String) { contacts.removeAll { it.id == id } }
        override fun getAllContacts() = emptyFlow<List<EmergencyContact>>()
        override suspend fun getAllContactsList() = contacts.toList()
        override fun getContactCount() = emptyFlow<Int>()
        override suspend fun getContactCountSync() = contacts.size
        override suspend fun deleteAll() { contacts.clear() }
    }

    private class TestKms : KeyManagementService(context = ContextWrapper(null)) {
        private var token: String? = null
        private var onboarding: Boolean = false
        private var anchor = BufferAnchor()

        override fun getAuthToken() = token
        override fun saveAuthToken(token: String) { this.token = token }
        override fun clearAuthSession() { token = null }

        override fun isOnboardingCompleted() = onboarding
        override fun setOnboardingCompleted(completed: Boolean) { onboarding = completed }

        override fun getBufferAnchor() = anchor
        override fun saveBufferAnchor(anchor: BufferAnchor) { this.anchor = anchor }
        override fun clearBufferAnchor() { anchor = BufferAnchor() }

        override fun encryptIncidentBundle(plainText: String): Pair<String, String> {
            return Pair("ENCRYPTED_AES_256_GCM_BUNDLE", "WRAPPED_DEK_KEY_BASE64")
        }

        override fun signData(data: String): Result<Pair<String, String>> {
            return Result.success(Pair("RSA_DIGITAL_SIGNATURE_BASE64", "RSA_PUBLIC_KEY_BASE64"))
        }

        override fun purgeAllEncryptedPreferencesAndKeys() {
            token = null
            onboarding = false
            anchor = BufferAnchor()
        }
    }

    private inner class TestBackendApi : AWSBackendApi {
        override suspend fun register(request: AuthRequest) = Response.success(AuthResponse("token", "user1"))
        override suspend fun login(request: AuthRequest) = Response.success(AuthResponse("token", "user1"))
        override suspend fun registerContact(contact: EmergencyContactDto) = Response.success(contact)

        override suspend fun uploadIncident(request: IncidentUploadRequest): Response<IncidentUploadResponse> {
            if (!isNetworkAvailable) throw UnknownHostException("DNS Failure: api.blackbox-safety.aws")
            return Response.success(IncidentUploadResponse("SUCCESS", request.incidentId, "S3 Uploaded", "s3_key_123"))
        }

        override suspend fun getIncident(id: String) = Response.success(IncidentDownloadResponse("id", 0L, "MANUAL", "bundle", "hash"))

        override suspend fun notifyContacts(id: String, request: NotificationRequest): Response<NotificationResponse> {
            if (!isNetworkAvailable) throw UnknownHostException("DNS Failure: api.blackbox-safety.aws")
            if (!isNotificationSuccessful) return Response.error(500, "SNS Error".toResponseBody())
            return Response.success(NotificationResponse("SENT", id, request.contacts.size))
        }
    }

    private lateinit var triggerDetector: TriggerDetector
    private lateinit var repository: BlackboxRepository
    private lateinit var kms: TestKms

    @Before
    fun setUp() {
        ProtectionStateManager.resetState()
        sensorEvents.clear()
        incidentReports.clear()
        emergencyContacts.clear()
        isNetworkAvailable = true
        isNotificationSuccessful = true

        triggerDetector = TriggerDetector()
        triggerDetector.resetState()
        triggerDetector.isAutoDetectionEnabled = true

        kms = TestKms()

        repository = BlackboxRepository(
            sensorEventDao = TestSensorDao(sensorEvents),
            incidentReportDao = TestIncidentDao(incidentReports),
            emergencyContactDao = TestContactDao(emergencyContacts),
            keyManagementService = kms,
            apiService = TestBackendApi()
        )
    }

    @Test
    fun testCompleteEndToEndSafetyLifecycle_AutoCrashAndDispatch() = runBlocking {
        // 1. App First Launch
        assertEquals(ProtectionState.STOPPED, ProtectionStateManager.state.value)
        assertFalse(kms.isOnboardingCompleted())

        // 2. Onboarding & Permission Completion
        kms.setOnboardingCompleted(true)
        assertTrue(kms.isOnboardingCompleted())

        // 3. Service Protection Startup
        ProtectionStateManager.updateState(ProtectionState.STARTING)
        ProtectionStateManager.updateState(ProtectionState.ACTIVE)
        assertEquals(ProtectionState.ACTIVE, ProtectionStateManager.state.value)

        // Add Emergency Contact
        val contact = EmergencyContact(id = "contact_001", name = "Alice", phone = "+15551234567", email = "alice@example.com", relationship = "Spouse", isEnabled = true, isPrimary = true)
        repository.insertEmergencyContact(contact)

        // 4. Record rolling sensor telemetry with SHA-256 chain
        repository.recordSensorEvent(EventType.ACCEL, """{"magnitude":9.81}""", System.currentTimeMillis() - 2000L)
        repository.recordSensorEvent(EventType.ACCEL, """{"magnitude":10.12}""", System.currentTimeMillis() - 1000L)

        // 5. Physics Trigger Evaluation: Valid Free-Fall + Impact Spike = AUTO_FALL
        val baseTime = System.currentTimeMillis()
        triggerDetector.evaluateMotion(1.0f, baseTime)
        triggerDetector.evaluateMotion(1.0f, baseTime + 50L)
        triggerDetector.evaluateMotion(1.0f, baseTime + 100L)
        triggerDetector.evaluateMotion(1.0f, baseTime + 200L)

        triggerDetector.evaluateMotion(8.0f, baseTime + 220L)
        triggerDetector.updateGyroscope(2.2f, baseTime + 250L)
        val triggered = triggerDetector.evaluateMotion(34.5f, baseTime + 280L)
        assertTrue("3-Stage physics model must trigger AUTO_FALL on sustained free-fall + impact", triggered)

        val countdownState = triggerDetector.countdownState.value
        assertTrue(countdownState is CountdownState.ActiveCountdown)
        assertEquals(TriggerType.AUTO_FALL, (countdownState as CountdownState.ActiveCountdown).triggerType)

        // 6. Countdown Expiration -> Incident Creation, Envelope Encryption & RSA Digital Signing
        triggerDetector.updateCountdown(0) // Expire
        assertEquals(CountdownState.Activated(TriggerType.AUTO_FALL), triggerDetector.countdownState.value)

        val report = repository.freezeBufferAndCreateIncident(
            triggerType = TriggerType.AUTO_FALL,
            windowMinutes = 60,
            timelineJson = "AUTO_CRASH timeline evidence payload"
        )

        // Assert Incident Creation & Cryptographic Hardening
        assertNotNull(report.id)
        assertEquals(TriggerType.AUTO_FALL, report.triggerType)
        assertEquals("ENCRYPTED_AES_256_GCM_BUNDLE", report.encryptedBundle)
        assertEquals("WRAPPED_DEK_KEY_BASE64", report.decryptionKey)
        assertEquals("RSA_DIGITAL_SIGNATURE_BASE64", report.digitalSignature)
        assertEquals("RSA_PUBLIC_KEY_BASE64", report.publicKeyBase64)
        assertEquals("contact_001", report.contactIdsJson)

        // 7. Verify Backend Dispatch Result
        val savedReport = incidentReports.first { it.id == report.id }
        assertEquals(UploadStatus.NOTIFIED, savedReport.uploadStatus)
        assertNull(savedReport.lastUploadError)
    }

    @Test
    fun testOfflineNetworkFailure_QueuesPendingReportAndPreservesContactSnapshotOnRetry() = runBlocking {
        // Configure contact snapshot
        repository.insertEmergencyContact(EmergencyContact(id = "contact_original", name = "Bob", phone = "+15559876543", relationship = "Parent", isEnabled = true))

        // Simulate Network Unavailable (DNS Failure)
        isNetworkAvailable = false

        val report = repository.freezeBufferAndCreateIncident(
            triggerType = TriggerType.AUTO_FALL,
            windowMinutes = 60,
            timelineJson = "AUTO_FALL timeline evidence payload"
        )

        // Assert report is preserved locally as PENDING
        val pendingReport = incidentReports.first { it.id == report.id }
        assertEquals(UploadStatus.PENDING, pendingReport.uploadStatus)
        assertTrue(pendingReport.lastUploadError!!.contains("DNS Failure"))
        assertEquals("contact_original", pendingReport.contactIdsJson)

        // User updates emergency contacts while offline
        repository.deleteEmergencyContact("contact_original")
        repository.insertEmergencyContact(EmergencyContact(id = "contact_new", name = "Charlie", phone = "+15551112222", relationship = "Friend", isEnabled = true))

        // Network recovers
        isNetworkAvailable = true

        // Retry pending uploads
        repository.retryPendingUploads()

        // Assert retry succeeded AND preserved original contact snapshot from incident creation time
        val retriedReport = incidentReports.first { it.id == report.id }
        assertEquals(UploadStatus.NOTIFIED, retriedReport.uploadStatus)
        assertEquals("contact_original", retriedReport.contactIdsJson)
    }

    @Test
    fun testSafetyCheckInTimerExpiration_TriggersEmergencyDispatch() = runBlocking {
        repository.insertEmergencyContact(EmergencyContact(id = "contact_checkin", name = "Dana", phone = "+15553334444", relationship = "Sibling", isEnabled = true))

        // Trigger manual check-in expiration dispatch
        val report = repository.freezeBufferAndCreateIncident(
            triggerType = TriggerType.MANUAL_SOS,
            windowMinutes = 60,
            timelineJson = "Solo Walk Safety Check-In Timer Expired"
        )

        assertEquals(TriggerType.MANUAL_SOS, report.triggerType)
        val savedReport = incidentReports.first { it.id == report.id }
        assertEquals(UploadStatus.NOTIFIED, savedReport.uploadStatus)
    }

    @Test
    fun testFactoryResetClearsAllDataAndRestoresStoppedFirstRunState() = runBlocking {
        kms.setOnboardingCompleted(true)
        ProtectionStateManager.updateState(ProtectionState.ACTIVE)
        repository.insertEmergencyContact(EmergencyContact(id = "c1", name = "Test", phone = "+1555", relationship = "Other", isEnabled = true))
        repository.recordSensorEvent(EventType.ACCEL, "{}", System.currentTimeMillis())

        // Execute Factory Reset
        ProtectionStateManager.updateState(ProtectionState.STOPPED)
        repository.factoryResetAllData()

        // Assert all local data is purged
        assertTrue(sensorEvents.isEmpty())
        assertTrue(incidentReports.isEmpty())
        assertTrue(emergencyContacts.isEmpty())
        assertFalse(kms.isOnboardingCompleted())
        assertEquals(ProtectionState.STOPPED, ProtectionStateManager.state.value)
    }
}
