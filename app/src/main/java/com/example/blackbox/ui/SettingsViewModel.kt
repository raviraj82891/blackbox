package com.example.blackbox.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blackbox.data.crypto.KeyManagementService
import com.example.blackbox.data.crypto.MedicalIdData
import com.example.blackbox.data.repository.BlackboxRepository
import com.example.blackbox.domain.trigger.TriggerDetector
import com.example.blackbox.sensor.SensorCollector
import com.example.blackbox.service.BlackboxForegroundService
import com.example.blackbox.util.CalibrationCalculator
import com.example.blackbox.util.CalibrationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val repository: BlackboxRepository,
    val keyManagementService: KeyManagementService,
    val triggerDetector: TriggerDetector
) : ViewModel() {

    private val _isCalibrating = MutableStateFlow(false)
    val isCalibrating: StateFlow<Boolean> = _isCalibrating.asStateFlow()

    private val _calibrationProgress = MutableStateFlow(0f)
    val calibrationProgress: StateFlow<Float> = _calibrationProgress.asStateFlow()

    private val _calibrationResultMessage = MutableStateFlow<String?>(keyManagementService.getLastCalibrationSummary())
    val calibrationResultMessage: StateFlow<String?> = _calibrationResultMessage.asStateFlow()

    private val _medicalIdData = MutableStateFlow(keyManagementService.getMedicalIdData())
    val medicalIdData: StateFlow<MedicalIdData> = _medicalIdData.asStateFlow()

    private val _isAutoDetectionEnabled = MutableStateFlow(keyManagementService.isAutoDetectionEnabled())
    val isAutoDetectionEnabled: StateFlow<Boolean> = _isAutoDetectionEnabled.asStateFlow()

    init {
        triggerDetector.isAutoDetectionEnabled = keyManagementService.isAutoDetectionEnabled()
    }

    fun setAutoDetectionEnabled(enabled: Boolean) {
        keyManagementService.setAutoDetectionEnabled(enabled)
        triggerDetector.isAutoDetectionEnabled = enabled
        _isAutoDetectionEnabled.value = enabled
    }

    /**
     * Measures REAL accelerometer readings during a 10-second window,
     * calculates baseline statistics, and derives personalized crash threshold safely.
     */
    fun startPersonalCalibration() {
        viewModelScope.launch {
            _isCalibrating.value = true
            _calibrationProgress.value = 0f
            _calibrationResultMessage.value = "Calibrating... Please carry or hold phone naturally."

            val sensorCollector = SensorCollector(context)
            val collectedReadings = mutableListOf<Float>()

            val collectJob = launch(Dispatchers.IO) {
                sensorCollector.observeAccelerometer().collect { reading ->
                    collectedReadings.add(reading.magnitude)
                }
            }

            for (i in 1..10) {
                delay(1000)
                _calibrationProgress.value = i / 10f
            }

            collectJob.cancel()

            when (val result = CalibrationCalculator.calculateBaselineAndThreshold(collectedReadings)) {
                is CalibrationResult.Success -> {
                    triggerDetector.impactThresholdMs2 = result.derivedImpactThreshold
                    keyManagementService.saveCalibrationResult(result.meanMagnitude, result.derivedImpactThreshold, result.timestampMs)
                    _calibrationResultMessage.value = "Calibration Complete! Measured baseline: %.1f m/s² (stdDev ±%.1f). Personalized crash threshold set to %.1f m/s².".format(
                        result.meanMagnitude, result.stdDevMagnitude, result.derivedImpactThreshold
                    )
                }
                is CalibrationResult.AbnormalMotion -> {
                    _calibrationResultMessage.value = result.reason
                }
                is CalibrationResult.Error -> {
                    _calibrationResultMessage.value = "Calibration Notice: ${result.reason}"
                }
            }

            _isCalibrating.value = false
        }
    }

    fun saveMedicalIdData(data: MedicalIdData) {
        keyManagementService.saveMedicalIdData(data)
        _medicalIdData.value = data
    }

    fun clearSensorBuffer() {
        viewModelScope.launch { repository.clearSensorBuffer() }
    }

    fun deleteIncidentHistory() {
        viewModelScope.launch { repository.deleteIncidentHistory() }
    }

    fun deleteAllEmergencyContacts() {
        viewModelScope.launch { repository.deleteAllEmergencyContacts() }
    }

    fun factoryResetAllData(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            // 1. Stop background protection service before data destruction
            val intent = Intent(context, BlackboxForegroundService::class.java).apply {
                action = BlackboxForegroundService.ACTION_STOP_SERVICE
            }
            runCatching { context.startService(intent) }

            // 2. Perform complete data destruction across database & encrypted preferences
            repository.factoryResetAllData()

            // 3. Reset ViewModel UI state
            _calibrationResultMessage.value = null
            _medicalIdData.value = MedicalIdData()

            // 4. Trigger reinitialization callback
            onComplete()
        }
    }

    fun wipeAllData(onComplete: () -> Unit = {}) {
        factoryResetAllData(onComplete)
    }
}
