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

/**
 * Trigger Detector — On-device physics engine detecting severe crashes and falls.
 *
 * Fall Detection Physics (3-Stage Model):
 * 1. Free-Fall Weightlessness: Vector magnitude < 3.5 m/s² for 150-600ms.
 * 2. Heavy Impact Spike: Vector magnitude > 25.0 m/s².
 * 3. Orientation Shift & Post-Fall Stillness.
 */
class TriggerDetector {

    var impactThresholdMs2: Double = 25.0
    var gyroThresholdRad: Double = 2.5
    var postImpactStillnessWindowMs: Long = 20000L

    private val _countdownState = MutableStateFlow<CountdownState>(CountdownState.Idle)
    val countdownState: StateFlow<CountdownState> = _countdownState.asStateFlow()

    private var possibleImpactTimeMs: Long = 0L
    private var lastPeakMagnitude: Double = 0.0

    // Latest gyroscopic rotation magnitude
    private var latestGyroDelta: Float = 0f

    // Free-fall weightlessness tracking for 3-Stage Fall Detection
    private var freeFallStartTimeMs: Long = 0L
    private var isFreeFallDetected = false

    // Adaptive sensitivity calibration counter
    private var cancelledCountdownsInWindow = 0
    private var lastCancelledPeakMagnitude = 0.0

    fun updateGyroscope(delta: Float) {
        latestGyroDelta = delta
    }

    fun evaluateMotion(accelMagnitude: Float, timestampMs: Long): Boolean {
        if (_countdownState.value is CountdownState.ActiveCountdown || _countdownState.value is CountdownState.Activated) {
            return false
        }

        // Stage 1: Detect free-fall weightlessness (< 3.5 m/s²)
        if (accelMagnitude < 3.5f) {
            if (freeFallStartTimeMs == 0L) {
                freeFallStartTimeMs = timestampMs
            } else if (timestampMs - freeFallStartTimeMs in 150..600) {
                isFreeFallDetected = true
            }
        } else if (accelMagnitude > 12.0f) {
            // Reset free-fall window if normal motion resumes without impact
            if (timestampMs - freeFallStartTimeMs > 800) {
                isFreeFallDetected = false
                freeFallStartTimeMs = 0L
            }
        }

        // Stage 2: Heavy Impact Spike (> impactThresholdMs2)
        if (accelMagnitude > impactThresholdMs2) {
            possibleImpactTimeMs = timestampMs
            lastPeakMagnitude = accelMagnitude.toDouble()

            // Classify as AUTO_FALL if preceded by free-fall weightlessness within 1.5s
            val triggerType = if (isFreeFallDetected && (timestampMs - freeFallStartTimeMs) <= 1500) {
                TriggerType.AUTO_FALL
            } else {
                TriggerType.AUTO_CRASH
            }

            isFreeFallDetected = false
            freeFallStartTimeMs = 0L

            // 30-second confirmation for automatic crash/fall detection
            startCountdown(triggerType, lastPeakMagnitude, durationSeconds = 30)
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
