package com.example.blackbox.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.blackbox.data.db.EventType
import com.example.blackbox.data.db.TriggerType
import com.example.blackbox.data.repository.BlackboxRepository
import com.example.blackbox.domain.fusion.FusionEngine
import com.example.blackbox.domain.trigger.CountdownState
import com.example.blackbox.domain.trigger.TriggerDetector
import com.example.blackbox.sensor.BatteryCollector
import com.example.blackbox.service.BlackboxForegroundService
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

    private val batteryCollector = BatteryCollector(application)

    private val _isServiceRunning = MutableStateFlow(true)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    private val _isBatterySaverActive = MutableStateFlow(false)
    val isBatterySaverActive: StateFlow<Boolean> = _isBatterySaverActive.asStateFlow()

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
                "IN_VEHICLE" -> "In Vehicle — Actively Monitoring High-Speed Motion"
                "WALKING" -> "Walking — Normal Motion Monitoring"
                "RUNNING" -> "Running — High Dynamic Motion Active"
                else -> "Stationary — Low Activity Baseline"
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Stationary — Low Activity Baseline")

    // Adaptive threshold suggestion prompt state
    private val _suggestAdaptiveThreshold = MutableStateFlow(false)
    val suggestAdaptiveThreshold: StateFlow<Boolean> = _suggestAdaptiveThreshold.asStateFlow()

    private var countdownTimerJob: Job? = null

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

        // Periodically check battery status for Power Saver Mode
        viewModelScope.launch {
            while (true) {
                val battery = batteryCollector.getBatteryStatus()
                _isBatterySaverActive.value = battery.levelPercentage < 15 && !battery.isCharging
                delay(30000)
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
            repository.freezeBufferAndCreateIncident(
                triggerType = triggerType,
                windowMinutes = 60,
                timelineJson = timelineJson
            )
            triggerDetector.resetState()
        }
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
        // Immediate 3-second quick activation for deliberate manual SOS
        triggerDetector.startCountdown(TriggerType.MANUAL_SOS, durationSeconds = 3)
    }

    fun wipeAllData() {
        viewModelScope.launch {
            repository.wipeAllData()
        }
    }
}
