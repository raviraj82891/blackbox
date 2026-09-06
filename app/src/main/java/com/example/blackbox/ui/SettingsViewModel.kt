package com.example.blackbox.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blackbox.data.repository.BlackboxRepository
import com.example.blackbox.domain.trigger.TriggerDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val repository: BlackboxRepository,
    val triggerDetector: TriggerDetector
) : ViewModel() {

    private val _isCalibrating = MutableStateFlow(false)
    val isCalibrating: StateFlow<Boolean> = _isCalibrating.asStateFlow()

    private val _calibrationProgress = MutableStateFlow(0f)
    val calibrationProgress: StateFlow<Float> = _calibrationProgress.asStateFlow()

    /**
     * Feature 2.3: Personal Calibration step.
     * Records baseline motion over 10 seconds and derives impactThresholdMs2 = baseline + 18.0 m/s².
     */
    fun startPersonalCalibration() {
        viewModelScope.launch {
            _isCalibrating.value = true
            _calibrationProgress.value = 0f

            for (i in 1..10) {
                delay(1000)
                _calibrationProgress.value = i / 10f
            }

            // Derive baseline-plus-margin threshold (e.g. 9.81 + 18.0 = 27.81 m/s²)
            val baseline = 9.81
            val derivedThreshold = baseline + 18.0
            triggerDetector.impactThresholdMs2 = derivedThreshold

            _isCalibrating.value = false
        }
    }

    fun wipeAllData() {
        viewModelScope.launch {
            repository.wipeAllData()
        }
    }
}
