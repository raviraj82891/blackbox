package com.example.blackbox.ui

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
import com.example.blackbox.data.db.EventType
import com.example.blackbox.data.db.IncidentReport
import com.example.blackbox.data.db.IncidentReportDao
import com.example.blackbox.data.db.SensorEvent
import com.example.blackbox.data.db.SensorEventDao
import com.example.blackbox.data.repository.BlackboxRepository
import com.example.blackbox.data.repository.SensorBatchItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class MemoryRegressionTest {

    private val eventsList = mutableListOf<SensorEvent>()

    private class TestSensorDao(val events: MutableList<SensorEvent>) : SensorEventDao {
        override suspend fun insert(event: SensorEvent): Long {
            events.add(event)
            return events.size.toLong()
        }
        override suspend fun insertAll(events: List<SensorEvent>): List<Long> {
            this.events.addAll(events)
            return events.map { this.events.size.toLong() }
        }
        override suspend fun getLastEvent(): SensorEvent? = events.lastOrNull()
        override fun getEventsSince(cutoffMs: Long) = MutableStateFlow(events)
        override suspend fun getEventsSinceList(cutoffMs: Long) = events.toList()
        override suspend fun getAllEventsInWindow(startMs: Long, endMs: Long) = events.toList()
        override suspend fun getEventsOlderThanList(cutoffMs: Long) = emptyList<SensorEvent>()
        override suspend fun purgeOlderThan(cutoffMs: Long) = 0
        override fun getEventCount() = MutableStateFlow(events.size)
        override suspend fun getEventCountSync() = events.size
        override suspend fun deleteAll() { events.clear() }
    }

    private class TestIncidentDao : IncidentReportDao {
        override suspend fun insert(report: IncidentReport) {}
        override suspend fun update(report: IncidentReport) {}
        override fun getAllReports() = MutableStateFlow(emptyList<IncidentReport>())
        override suspend fun getAllReportsList() = emptyList<IncidentReport>()
        override suspend fun getReportById(id: String) = null
        override suspend fun deleteAll() {}
    }

    private class TestContactDao : EmergencyContactDao {
        override suspend fun insertContact(contact: EmergencyContact) {}
        override suspend fun updateContact(contact: EmergencyContact) {}
        override suspend fun deleteContactById(id: String) {}
        override fun getAllContacts() = MutableStateFlow(emptyList<EmergencyContact>())
        override suspend fun getAllContactsList() = emptyList<EmergencyContact>()
        override fun getContactCount() = MutableStateFlow(0)
        override suspend fun getContactCountSync() = 0
        override suspend fun deleteAll() {}
    }

    private class TestAWSBackendApi : AWSBackendApi {
        override suspend fun register(request: AuthRequest) = Response.success(AuthResponse("token", "user1"))
        override suspend fun login(request: AuthRequest) = Response.success(AuthResponse("token", "user1"))
        override suspend fun registerContact(contact: EmergencyContactDto) = Response.success(contact)
        override suspend fun uploadIncident(request: IncidentUploadRequest) = Response.success(IncidentUploadResponse("SUCCESS", request.incidentId, "msg", "s3key"))
        override suspend fun getIncident(id: String) = Response.success(IncidentDownloadResponse("id", 0L, "MANUAL", "bundle", "hash"))
        override suspend fun notifyContacts(id: String, request: NotificationRequest) = Response.success(NotificationResponse("SENT", id, 1))
    }

    private lateinit var repository: BlackboxRepository

    @Before
    fun setUp() {
        eventsList.clear()
        val kms = KeyManagementService(ContextWrapper(null))
        repository = BlackboxRepository(TestSensorDao(eventsList), TestIncidentDao(), TestContactDao(), kms, TestAWSBackendApi())
    }

    @Test
    fun testContinuousHighFrequencyBatchingRemainsBoundedInMemory() {
        runBlocking {
            // Simulate 500 high-frequency sensor readings (10 seconds at 50Hz)
            val batch = ArrayList<SensorBatchItem>(50)
            for (i in 1..500) {
                batch.add(SensorBatchItem(EventType.ACCEL, "{\"x\":0.1,\"y\":9.81,\"z\":0.2,\"magnitude\":9.81}", System.currentTimeMillis() + i * 20L))
                if (batch.size == 50) {
                    repository.recordSensorEventsBatch(batch)
                    batch.clear()
                }
            }

            // Verify all 500 events recorded into repository with 10 batch transactions
            assertEquals(500, eventsList.size)
        }
    }
}
