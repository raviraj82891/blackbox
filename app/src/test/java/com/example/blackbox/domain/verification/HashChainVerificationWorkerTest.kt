package com.example.blackbox.domain.verification

import android.content.Context
import android.content.ContextWrapper
import androidx.work.Data
import androidx.work.ForegroundUpdater
import androidx.work.ListenableWorker
import androidx.work.ProgressUpdater
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.impl.utils.taskexecutor.SerialExecutor
import androidx.work.impl.utils.taskexecutor.TaskExecutor
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
import com.example.blackbox.data.db.IncidentReport
import com.example.blackbox.data.db.IncidentReportDao
import com.example.blackbox.data.db.SensorEvent
import com.example.blackbox.data.db.SensorEventDao
import com.example.blackbox.data.repository.BlackboxRepository
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.lang.reflect.Proxy
import java.util.Collection
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class HashChainVerificationWorkerTest {

    private class TestSensorDao : SensorEventDao {
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
        override suspend fun deleteAll() {}
    }

    private class TestIncidentDao : IncidentReportDao {
        override suspend fun insert(report: IncidentReport) {}
        override suspend fun update(report: IncidentReport) {}
        override fun getAllReports() = emptyFlow<List<IncidentReport>>()
        override suspend fun getAllReportsList() = emptyList<IncidentReport>()
        override suspend fun getReportById(id: String) = null
        override suspend fun deleteAll() {}
    }

    private class TestContactDao : EmergencyContactDao {
        override suspend fun insertContact(contact: EmergencyContact) {}
        override suspend fun updateContact(contact: EmergencyContact) {}
        override suspend fun deleteContactById(id: String) {}
        override fun getAllContacts() = emptyFlow<List<EmergencyContact>>()
        override suspend fun getAllContactsList() = emptyList<EmergencyContact>()
        override fun getContactCount() = emptyFlow<Int>()
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

    private class TestKms(context: Context) : KeyManagementService(context) {
        override fun getBufferAnchor() = BufferAnchor()
        override fun saveBufferAnchor(anchor: BufferAnchor) {}
    }

    private lateinit var context: Context
    private lateinit var repository: BlackboxRepository
    private lateinit var workerParams: WorkerParameters

    @Before
    fun setUp() {
        context = ContextWrapper(null)
        workerParams = createDummyWorkerParameters()
        repository = BlackboxRepository(TestSensorDao(), TestIncidentDao(), TestContactDao(), TestKms(context), TestAWSBackendApi())
    }

    @Test
    fun testHashChainVerificationWorkerConstructionAndExecution() = runBlocking {
        val worker = HashChainVerificationWorker(context, workerParams, repository)
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Suppress("UNCHECKED_CAST")
    private fun createDummyWorkerParameters(): WorkerParameters {
        val constructor = WorkerParameters::class.java.declaredConstructors[0]
        constructor.isAccessible = true
        val types = constructor.parameterTypes
        val args = arrayOfNulls<Any>(types.size)

        val futureProxy = Proxy.newProxyInstance(
            ListenableFuture::class.java.classLoader,
            arrayOf(ListenableFuture::class.java)
        ) { _, _, _ -> null } as ListenableFuture<Void>

        val taskExecutor = object : TaskExecutor {
            val mainExec = Executors.newSingleThreadExecutor()
            val serialExec = object : SerialExecutor {
                override fun execute(r: Runnable) { r.run() }
                override fun hasPendingTasks(): Boolean = false
            }
            override fun getMainThreadExecutor(): Executor = mainExec
            override fun getSerialTaskExecutor(): SerialExecutor = serialExec
        }

        for (i in types.indices) {
            when {
                types[i] == UUID::class.java -> args[i] = UUID.randomUUID()
                types[i] == Data::class.java -> args[i] = Data.EMPTY
                Collection::class.java.isAssignableFrom(types[i]) -> args[i] = HashSet<String>()
                types[i] == TaskExecutor::class.java -> args[i] = taskExecutor
                types[i] == Executor::class.java -> args[i] = Executors.newSingleThreadExecutor()
                types[i] == CoroutineDispatcher::class.java -> args[i] = Dispatchers.Default
                types[i] == WorkerFactory::class.java -> args[i] = WorkerFactory.getDefaultWorkerFactory()
                types[i] == ProgressUpdater::class.java -> args[i] = ProgressUpdater { _, _, _ -> futureProxy }
                types[i] == ForegroundUpdater::class.java -> args[i] = ForegroundUpdater { _, _, _ -> futureProxy }
                types[i] == Int::class.javaPrimitiveType -> args[i] = 1
                types[i] == Long::class.javaPrimitiveType -> args[i] = 0L
                types[i] == Boolean::class.javaPrimitiveType -> args[i] = false
                else -> args[i] = null
            }
        }
        return constructor.newInstance(*args) as WorkerParameters
    }
}
