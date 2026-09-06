package com.example.blackbox.domain.trigger

import com.example.blackbox.data.db.TriggerType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class CountdownState {
    object Idle : CountdownState()
    data class ActiveCountdown(
        val secondsRemaining: Int,
        val triggerType: TriggerType,
        val impactMagnitude: Double
    ) : CountdownState()
    data class Activated(
        val triggerType: TriggerType
    ) : CountdownState()
    object Cancelled : CountdownState()
}

class TriggerDetector {

    var impactThresholdMs2: Double = 28.0
    var gyroThresholdRad: Double = 2.5
    var postImpactStillnessWindowMs: Long = 20000L

    private val _countdownState = MutableStateFlow<CountdownState>(CountdownState.Idle)
    val countdownState: StateFlow<CountdownState> = _countdownState.asStateFlow()

    private var possibleImpactTimeMs: Long = 0L
    private var lastPeakMagnitude: Double = 0.0

    // Adaptive sensitivity calibration counter
    private var cancelledCountdownsInWindow = 0
    private var lastCancelledPeakMagnitude = 0.0

    fun evaluateMotion(accelMagnitude: Float, gyroDelta: Float, timestampMs: Long): Boolean {
        if (_countdownState.value is CountdownState.ActiveCountdown || _countdownState.value is CountdownState.Activated) {
            return false
        }

        if (accelMagnitude > impactThresholdMs2 && gyroDelta > gyroThresholdRad) {
            possibleImpactTimeMs = timestampMs
            lastPeakMagnitude = accelMagnitude.toDouble()
            // 30-second confirmation for automatic crash/fall detection
            startCountdown(TriggerType.AUTO_CRASH, lastPeakMagnitude, durationSeconds = 30)
            return true
        }
        return false
    }

    fun startCountdown(triggerType: TriggerType, magnitude: Double = 0.0, durationSeconds: Int = 30) {
        val duration = if (triggerType == TriggerType.MANUAL_SOS) 3 else durationSeconds
        _countdownState.value = CountdownState.ActiveCountdown(
            secondsRemaining = duration,
            triggerType = triggerType,
            impactMagnitude = magnitude
        )
    }

    fun updateCountdown(secondsLeft: Int) {
        val current = _countdownState.value
        if (current is CountdownState.ActiveCountdown) {
            if (secondsLeft <= 0) {
                _countdownState.value = CountdownState.Activated(current.triggerType)
            } else {
                _countdownState.value = current.copy(secondsRemaining = secondsLeft)
            }
        }
    }

    fun cancelCountdown() {
        val current = _countdownState.value
        if (current is CountdownState.ActiveCountdown && current.triggerType != TriggerType.MANUAL_SOS) {
            cancelledCountdownsInWindow++
            lastCancelledPeakMagnitude = current.impactMagnitude
        }
        _countdownState.value = CountdownState.Cancelled
        _countdownState.value = CountdownState.Idle
    }

    fun shouldSuggestThresholdAdjustment(): Boolean = cancelledCountdownsInWindow >= 3

    fun getSuggestedNewThreshold(): Double = (lastCancelledPeakMagnitude + 2.0).coerceAtLeast(32.0)

    fun resetAdaptiveThresholdCounter() {
        cancelledCountdownsInWindow = 0
    }

    fun resetState() {
        _countdownState.value = CountdownState.Idle
    }
}
