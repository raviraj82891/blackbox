package com.example.blackbox.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.blackbox.data.api.EmergencyContactDto
import com.example.blackbox.data.api.RetrofitClient
import com.example.blackbox.data.crypto.KeyManagementService
import com.example.blackbox.data.db.BlackboxDatabase
import com.example.blackbox.data.db.IncidentReport
import com.example.blackbox.data.db.SensorEvent
import com.example.blackbox.data.db.TriggerType
import com.example.blackbox.data.repository.BlackboxRepository
import com.example.blackbox.domain.fusion.FusionEngine
import com.example.blackbox.domain.fusion.TimelineEntry
import com.example.blackbox.domain.pdf.PdfReportGenerator
import com.example.blackbox.domain.trigger.CountdownState
import com.example.blackbox.domain.trigger.SimulatedTriggerEngine
import com.example.blackbox.domain.trigger.TriggerDetector
import com.example.blackbox.service.BlackboxForegroundService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val kms = KeyManagementService(application)
    private val dbPassphrase = kms.getOrCreateDatabasePassphrase()
    private val db = BlackboxDatabase.getInstance(application, dbPassphrase)
    val repository = BlackboxRepository(db.sensorEventDao(), db.incidentReportDao(), kms, RetrofitClient.apiService)

    val triggerDetector = TriggerDetector()
    val simulatedEngine = SimulatedTriggerEngine(repository, triggerDetector)
    val pdfGenerator = PdfReportGenerator(application)

    private val _isServiceRunning = MutableStateFlow(true)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    val bufferEventCount: StateFlow<Int> = repository.getBufferEventCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val rawEvents: StateFlow<List<SensorEvent>> = repository.getRollingBufferEvents(60)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reconstructedTimeline: StateFlow<List<TimelineEntry>> = combine(rawEvents) { (events) ->
        FusionEngine.reconstructTimeline(events)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val incidentReports: StateFlow<List<IncidentReport>> = repository.getAllIncidentReports()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val countdownState: StateFlow<CountdownState> = triggerDetector.countdownState

    private val _isChainIntegrityValid = MutableStateFlow(true)
    val isChainIntegrityValid: StateFlow<Boolean> = _isChainIntegrityValid.asStateFlow()

    private val _emergencyContacts = MutableStateFlow<List<EmergencyContactDto>>(
        listOf(
            EmergencyContactDto("1", "Emergency Contact 1", "+1234567890", "contact1@example.com", "Family"),
            EmergencyContactDto("2", "Emergency Contact 2", "+0987654321", "contact2@example.com", "Doctor")
        )
    )
    val emergencyContacts: StateFlow<List<EmergencyContactDto>> = _emergencyContacts.asStateFlow()

    private var countdownTimerJob: Job? = null

    init {
        // Observe countdown state to manage 30s confirmation timer
        viewModelScope.launch {
            countdownState.collectLatest { state ->
                if (state is CountdownState.ActiveCountdown) {
                    startCountdownTimer(state.secondsRemaining, state.triggerType)
                } else if (state is CountdownState.Activated) {
                    onCountdownExpired(state.triggerType)
                }
            }
        }

        // Periodically verify buffer hash chain integrity
        viewModelScope.launch {
            while (true) {
                _isChainIntegrityValid.value = repository.verifyBufferIntegrity(60)
                delay(10000)
            }
        }
    }

    private fun startCountdownTimer(initialSeconds: Int, triggerType: TriggerType) {
        countdownTimerJob?.cancel()
        countdownTimerJob = viewModelScope.launch {
            var left = initialSeconds
            while (left > 0) {
                delay(1000)
                left--
                triggerDetector.updateCountdown(left)
            }
        }
    }

    fun cancelCountdown() {
        countdownTimerJob?.cancel()
        triggerDetector.cancelCountdown()
    }

    private fun onCountdownExpired(triggerType: TriggerType) {
        viewModelScope.launch {
            val timelineEntries = reconstructedTimeline.value
            val timelineJson = timelineEntries.joinToString("\n") {
                "[${it.formattedTime}] ${it.summaryTitle}: ${it.detailedDescription}"
            }
            repository.freezeBufferAndCreateIncident(
                triggerType = triggerType,
                windowMinutes = 60,
                timelineJson = timelineJson
            )
            triggerDetector.resetState()
        }
    }

    fun startProtectionService() {
        val intent = Intent(getApplication(), BlackboxForegroundService::class.java)
        getApplication<Application>().startForegroundService(intent)
        _isServiceRunning.value = true
    }

    fun pauseProtectionService() {
        val intent = Intent(getApplication(), BlackboxForegroundService::class.java).apply {
            action = BlackboxForegroundService.ACTION_PAUSE
        }
        getApplication<Application>().startService(intent)
        _isServiceRunning.value = false
    }

    fun resumeProtectionService() {
        val intent = Intent(getApplication(), BlackboxForegroundService::class.java).apply {
            action = BlackboxForegroundService.ACTION_RESUME
        }
        getApplication<Application>().startService(intent)
        _isServiceRunning.value = true
    }

    fun triggerManualSos() {
        triggerDetector.startCountdown(TriggerType.MANUAL_SOS, 0.0)
    }

    fun wipeAllData() {
        viewModelScope.launch {
            repository.wipeAllData()
        }
    }

    fun simulateCrashSequence() {
        viewModelScope.launch {
            simulatedEngine.injectSimulatedCrashSequence()
        }
    }

    fun addEmergencyContact(contact: EmergencyContactDto) {
        _emergencyContacts.value = _emergencyContacts.value + contact
    }

    fun removeEmergencyContact(contactId: String) {
        _emergencyContacts.value = _emergencyContacts.value.filterNot { it.id == contactId }
    }

    fun exportPdfReport(report: IncidentReport): File {
        return pdfGenerator.generatePdfReport(report)
    }
}
