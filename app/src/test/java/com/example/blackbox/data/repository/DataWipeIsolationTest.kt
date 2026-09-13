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
import com.example.blackbox.data.crypto.KeyManagementService
import com.example.blackbox.data.db.EmergencyContact
import com.example.blackbox.data.db.EmergencyContactDao
import com.example.blackbox.data.db.IncidentReport
import com.example.blackbox.data.db.IncidentReportDao
import com.example.blackbox.data.db.SensorEvent
import com.example.blackbox.data.db.SensorEventDao
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class DataWipeIsolationTest {

    private var sensorBufferCleared = false
    private var incidentsCleared = false
    private var contactsCleared = false
    private var preferencesPurged = false

    private class TestSensorDao(val onClear: () -> Unit) : SensorEventDao {
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

    private class TestIncidentDao(val onClear: () -> Unit) : IncidentReportDao {
        override suspend fun insert(report: IncidentReport) {}
        override suspend fun update(report: IncidentReport) {}
        override fun getAllReports() = emptyFlow<List<IncidentReport>>()
        override suspend fun getAllReportsList() = emptyList<IncidentReport>()
        override suspend fun getReportById(id: String) = null
        override suspend fun deleteAll() { onClear() }
    }

    private class TestContactDao(val onClear: () -> Unit) : EmergencyContactDao {
        override suspend fun insertContact(contact: EmergencyContact) {}
        override suspend fun updateContact(contact: EmergencyContact) {}
        override suspend fun deleteContactById(id: String) {}
        override fun getAllContacts() = emptyFlow<List<EmergencyContact>>()
        override suspend fun getAllContactsList() = emptyList<EmergencyContact>()
        override fun getContactCount() = emptyFlow<Int>()
        override suspend fun getContactCountSync() = 0
        override suspend fun deleteAll() { onClear() }
    }

    private class TestAWSBackendApi : AWSBackendApi {
        override suspend fun register(request: AuthRequest) = Response.success(AuthResponse("token", "user1"))
        override suspend fun login(request: AuthRequest) = Response.success(AuthResponse("token", "user1"))
        override suspend fun registerContact(contact: EmergencyContactDto) = Response.success(contact)
        override suspend fun uploadIncident(request: IncidentUploadRequest) = Response.success(IncidentUploadResponse("SUCCESS", request.incidentId, "msg", "s3key"))
        override suspend fun getIncident(id: String) = Response.success(IncidentDownloadResponse("id", 0L, "MANUAL", "bundle", "hash"))
        override suspend fun notifyContacts(id: String, request: NotificationRequest) = Response.success(NotificationResponse("SENT", id, 1))
    }

    private class TestKms(val onPurge: () -> Unit) : KeyManagementService(context = ContextWrapper(null)) {
        override fun clearBufferAnchor() {}
        override fun purgeAllEncryptedPreferencesAndKeys() { onPurge() }
    }

    private lateinit var repository: BlackboxRepository

    @Before
    fun setUp() {
        sensorBufferCleared = false
        incidentsCleared = false
        contactsCleared = false
        preferencesPurged = false

        repository = BlackboxRepository(
            sensorEventDao = TestSensorDao { sensorBufferCleared = true },
            incidentReportDao = TestIncidentDao { incidentsCleared = true },
            emergencyContactDao = TestContactDao { contactsCleared = true },
            keyManagementService = TestKms { preferencesPurged = true },
            apiService = TestAWSBackendApi()
        )
    }

    @Test
    fun testClearBufferScopeIsolation() = runBlocking {
        repository.clearSensorBuffer()

        assertTrue("clearSensorBuffer must delete sensor events table", sensorBufferCleared)
        assertFalse("clearSensorBuffer must NOT delete incident reports", incidentsCleared)
        assertFalse("clearSensorBuffer must NOT delete emergency contacts", contactsCleared)
        assertFalse("clearSensorBuffer must NOT purge all preferences", preferencesPurged)
    }

    @Test
    fun testDeleteIncidentsScopeIsolation() = runBlocking {
        repository.deleteIncidentHistory()

        assertFalse("deleteIncidentHistory must NOT delete sensor buffer", sensorBufferCleared)
        assertTrue("deleteIncidentHistory must delete incident reports table", incidentsCleared)
        assertFalse("deleteIncidentHistory must NOT delete emergency contacts", contactsCleared)
        assertFalse("deleteIncidentHistory must NOT purge all preferences", preferencesPurged)
    }

    @Test
    fun testDeleteContactsScopeIsolation() = runBlocking {
        repository.deleteAllEmergencyContacts()

        assertFalse("deleteAllEmergencyContacts must NOT delete sensor buffer", sensorBufferCleared)
        assertFalse("deleteAllEmergencyContacts must NOT delete incident reports", incidentsCleared)
        assertTrue("deleteAllEmergencyContacts must delete emergency contacts table", contactsCleared)
        assertFalse("deleteAllEmergencyContacts must NOT purge all preferences", preferencesPurged)
    }
}
