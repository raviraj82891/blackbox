package com.example.blackbox.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.blackbox.data.crypto.KeyManagementService
import com.example.blackbox.data.db.EventType
import com.example.blackbox.data.db.IncidentReport
import com.example.blackbox.data.db.TriggerType
import com.example.blackbox.data.repository.BlackboxRepository
import com.example.blackbox.domain.trigger.CountdownState
import com.example.blackbox.domain.trigger.TriggerDetector
import com.example.blackbox.sensor.BatteryCollector
import com.example.blackbox.service.BlackboxForegroundService
import com.example.blackbox.service.ProtectionState
import com.example.blackbox.service.ProtectionStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    application: Application,
    val repository: BlackboxRepository,
    val triggerDetector: TriggerDetector
) : AndroidViewModel(application) {

    private val kms = KeyManagementService(application)
    private val batteryCollector = BatteryCollector(application)

    // Single Authoritative Protection State from ProtectionStateManager
    val protectionState: StateFlow<ProtectionState> = ProtectionStateManager.state
    val protectionError: StateFlow<String?> = ProtectionStateManager.lastError

    // Derived directly from ProtectionStateManager state (no duplicated or optimistic local truth)
    val isServiceRunning: StateFlow<Boolean> = protectionState
        .map { it == ProtectionState.ACTIVE || it == ProtectionState.STARTING }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _isBatterySaverActive = MutableStateFlow(false)
    val isBatterySaverActive: StateFlow<Boolean> = _isBatterySaverActive.asStateFlow()

    private val _batteryLevel = MutableStateFlow(100)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    // Explicit UNKNOWN/CHECKING state (null = checking/unknown, true = valid, false = invalid)
    private val _isChainValid = MutableStateFlow<Boolean?>(null)
    val isChainValid: StateFlow<Boolean?> = _isChainValid.asStateFlow()

    val bufferEventCount: StateFlow<Int> = repository.getBufferEventCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val savedContactCount: StateFlow<Int> = repository.getEmergencyContactCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val countdownState: StateFlow<CountdownState> = triggerDetector.countdownState

    // Live accelerometer points for real-time Sparkline chart
    val sparklinePoints: StateFlow<List<Float>> = repository.getRollingBufferEvents(5)
        .map { events ->
            events.filter { it.type == EventType.ACCEL }
                .takeLast(60)
                .mapNotNull {
                    runCatching { JSONObject(it.payloadJson).optDouble("magnitude", 9.81).toFloat() }.getOrNull()
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Situational status derived from activity state
    val situationalStatus: StateFlow<String> = repository.getRollingBufferEvents(10)
        .map { events ->
            val lastActivity = events.lastOrNull { it.type == EventType.ACTIVITY }
            val state = if (lastActivity != null) {
                runCatching { JSONObject(lastActivity.payloadJson).optString("state", "STILL") }.getOrDefault("STILL")
            } else "STILL"

            when (state) {
                "IN_VEHICLE" -> "In Vehicle — Monitoring High-Speed Motion"
                "WALKING" -> "Walking — Normal Motion Active"
                "RUNNING" -> "Running — Dynamic Activity Active"
                else -> "Stationary — Normal Baseline"
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Stationary — Normal Baseline")

    // Adaptive threshold suggestion prompt state
    private val _suggestAdaptiveThreshold = MutableStateFlow(false)
    val suggestAdaptiveThreshold: StateFlow<Boolean> = _suggestAdaptiveThreshold.asStateFlow()

    // Solo Walk Safety Check-In Timer state
    private val _safetyTimerSeconds = MutableStateFlow<Int?>(null)
    val safetyTimerSeconds: StateFlow<Int?> = _safetyTimerSeconds.asStateFlow()

    // Post-expiration report result state for Alert Sent / Queued / Failed overlay
    private val _lastActivatedReport = MutableStateFlow<IncidentReport?>(null)
    val lastActivatedReport: StateFlow<IncidentReport?> = _lastActivatedReport.asStateFlow()

    fun clearLastActivatedReport() {
        _lastActivatedReport.value = null
    }

    private var countdownTimerJob: Job? = null
    private var safetyCheckInJob: Job? = null

    init {
        // Observe countdown state
        viewModelScope.launch {
            countdownState.collectLatest { state ->
                if (state is CountdownState.ActiveCountdown) {
                    startCountdownTimer(state.secondsRemaining, state.triggerType)
                } else if (state is CountdownState.Activated) {
                    onCountdownExpired(state.triggerType)
                }
            }
        }

        // Verify real cryptographic hash-chain integrity periodically
        viewModelScope.launch {
            while (true) {
                _isChainValid.value = repository.verifyBufferIntegrity(60)
                delay(15000)
            }
        }

        // Periodically check battery status for Power Saver Mode & Live Battery Level
        viewModelScope.launch {
            while (true) {
                val battery = batteryCollector.getBatteryStatus()
                _batteryLevel.value = battery.levelPercentage
                _isBatterySaverActive.value = battery.levelPercentage < 15 && !battery.isCharging
                delay(5000)
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

    fun startSafetyCheckInTimer(minutes: Int) {
        safetyCheckInJob?.cancel()
        _safetyTimerSeconds.value = minutes * 60

        safetyCheckInJob = viewModelScope.launch {
            while ((_safetyTimerSeconds.value ?: 0) > 0) {
                delay(1000)
                val current = _safetyTimerSeconds.value ?: 0
                val next = current - 1
                _safetyTimerSeconds.value = next
                if (next <= 0) {
                    kms.incrementSafetyCheckInCount()
                    triggerManualSos()
                    _safetyTimerSeconds.value = null
                }
            }
        }
    }

    fun cancelSafetyCheckInTimer() {
        safetyCheckInJob?.cancel()
        _safetyTimerSeconds.value = null
    }

    fun cancelCountdown() {
        countdownTimerJob?.cancel()
        kms.incrementCancelledCountdowns()
        triggerDetector.cancelCountdown()
        if (triggerDetector.shouldSuggestThresholdAdjustment()) {
            _suggestAdaptiveThreshold.value = true
        }
    }

    fun applyAdaptiveThreshold() {
        val newThreshold = triggerDetector.getSuggestedNewThreshold()
        triggerDetector.impactThresholdMs2 = newThreshold
        triggerDetector.resetAdaptiveThresholdCounter()
        _suggestAdaptiveThreshold.value = false
    }

    fun dismissAdaptivePrompt() {
        triggerDetector.resetAdaptiveThresholdCounter()
        _suggestAdaptiveThreshold.value = false
    }

    private fun onCountdownExpired(triggerType: TriggerType) {
        viewModelScope.launch {
            val timelineJson = "Emergency Incident Activated via $triggerType"
            val report = repository.freezeBufferAndCreateIncident(
                triggerType = triggerType,
                windowMinutes = 60,
                timelineJson = timelineJson
            )
            _lastActivatedReport.value = report
            triggerDetector.resetState()
        }
    }

    fun pauseProtectionService() {
        val intent = Intent(getApplication(), BlackboxForegroundService::class.java).apply {
            action = BlackboxForegroundService.ACTION_PAUSE
        }
        getApplication<Application>().startService(intent)
    }

    fun resumeProtectionService() {
        val intent = Intent(getApplication(), BlackboxForegroundService::class.java).apply {
            action = BlackboxForegroundService.ACTION_RESUME
        }
        getApplication<Application>().startService(intent)
    }

    fun triggerManualSos() {
        // Immediate 3-second quick activation for deliberate manual SOS
        triggerDetector.startCountdown(TriggerType.MANUAL_SOS, durationSeconds = 3)
    }

    fun wipeAllData() {
        viewModelScope.launch {
            repository.wipeAllData()
        }
    }
}
