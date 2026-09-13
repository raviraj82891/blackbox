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
 * Trigger Detector — On-device physics engine detecting severe crashes and falls
 * using a complete 3-Stage Multi-Evidence Physics Model.
 *
 * 3-Stage Model:
 * 1. Free-Fall Weightlessness: Vector magnitude < 3.5 m/s² for 150-600ms.
 * 2. Heavy Impact Spike: Vector magnitude > impactThresholdMs2 (default 20.0 m/s²).
 * 3. Orientation Shift & Gyroscopic Angular Rotation: Gyro delta > gyroThresholdRad (default 2.5 rad/s).
 */
class TriggerDetector {

    var impactThresholdMs2: Double = 20.0
    var gyroThresholdRad: Double = 2.5
    var postImpactStillnessWindowMs: Long = 20000L

    private val _countdownState = MutableStateFlow<CountdownState>(CountdownState.Idle)
    val countdownState: StateFlow<CountdownState> = _countdownState.asStateFlow()

    private var possibleImpactTimeMs: Long = 0L
    private var lastPeakMagnitude: Double = 0.0

    // Latest gyroscopic rotation magnitude (rad/s) & timestamp
    private var latestGyroDelta: Float = 0f
    private var latestGyroTimestampMs: Long = 0L

    // Free-fall weightlessness tracking for 3-Stage Fall Detection
    private var freeFallStartTimeMs: Long = 0L
    private var isFreeFallDetected = false

    // Adaptive sensitivity calibration counter
    private var cancelledCountdownsInWindow = 0
    private var lastCancelledPeakMagnitude = 0.0

    // Master toggle for automatic crash/fall evaluation
    var isAutoDetectionEnabled: Boolean = true

    fun updateGyroscope(delta: Float, timestampMs: Long = System.currentTimeMillis()) {
        latestGyroDelta = delta
        latestGyroTimestampMs = timestampMs
    }

    /**
     * Evaluates incoming accelerometer vector magnitude using the 3-Stage Multi-Evidence Physics Model.
     * Returns true ONLY if genuine multi-stage fall or crash evidence is confirmed.
     */
    fun evaluateMotion(accelMagnitude: Float, timestampMs: Long): Boolean {
        // Skip automatic crash/fall evaluation if master auto-detection toggle is disabled
        if (!isAutoDetectionEnabled) {
            return false
        }

        // Prevent duplicate triggers while a countdown or activation is already in progress
        if (_countdownState.value is CountdownState.ActiveCountdown || _countdownState.value is CountdownState.Activated) {
            return false
        }

        // Stage 1: Detect free-fall weightlessness (< 3.5 m/s² for 150-600ms)
        if (accelMagnitude < 3.5f) {
            if (freeFallStartTimeMs == 0L) {
                freeFallStartTimeMs = timestampMs
            } else if (timestampMs - freeFallStartTimeMs in 150..600) {
                isFreeFallDetected = true
            }
        } else if (accelMagnitude > 12.0f) {
            // Reset free-fall window if normal motion resumes without impact within 800ms
            if (timestampMs - freeFallStartTimeMs > 800) {
                isFreeFallDetected = false
                freeFallStartTimeMs = 0L
            }
        }

        // Stage 2: Heavy Impact Spike (> impactThresholdMs2)
        if (accelMagnitude > impactThresholdMs2) {
            possibleImpactTimeMs = timestampMs
            lastPeakMagnitude = accelMagnitude.toDouble()

            val isRecentFreeFall = isFreeFallDetected && (timestampMs - freeFallStartTimeMs) <= 1500
            val isRecentGyroRotation = (latestGyroDelta >= gyroThresholdRad.toFloat()) &&
                    (latestGyroTimestampMs == 0L || (timestampMs - latestGyroTimestampMs) <= 1500)

            // Multi-Stage Evidence Classification:
            // 1. AUTO_FALL: Stage 1 (Free-fall) + Stage 2 (Impact)
            // 2. AUTO_CRASH: Stage 2 (Impact) + Stage 3 (Angular Rotation / Gyro Delta >= gyroThresholdRad)
            val triggerType: TriggerType? = when {
                isRecentFreeFall -> TriggerType.AUTO_FALL
                isRecentGyroRotation -> TriggerType.AUTO_CRASH
                else -> null // Single accel spike without free-fall OR gyro rotation is treated as hard table drop / bumped phone and filtered out
            }

            if (triggerType != null) {
                isFreeFallDetected = false
                freeFallStartTimeMs = 0L

                // 30-second confirmation countdown for automatic crash/fall detection
                startCountdown(triggerType, lastPeakMagnitude, durationSeconds = 30)
                return true
            }
        }
        return false
    }

    /**
     * Initiates confirmation countdown. Manual SOS bypasses multi-stage physics evaluation
     * and uses an independent 3-second quick path.
     */
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
        resetInternalState()
        _countdownState.value = CountdownState.Cancelled
        _countdownState.value = CountdownState.Idle
    }

    fun shouldSuggestThresholdAdjustment(): Boolean = cancelledCountdownsInWindow >= 3

    fun getSuggestedNewThreshold(): Double = (lastCancelledPeakMagnitude + 2.0).coerceAtLeast(32.0)

    fun resetAdaptiveThresholdCounter() {
        cancelledCountdownsInWindow = 0
    }

    private fun resetInternalState() {
        isFreeFallDetected = false
        freeFallStartTimeMs = 0L
        latestGyroDelta = 0f
        latestGyroTimestampMs = 0L
        possibleImpactTimeMs = 0L
    }

    fun resetState() {
        resetInternalState()
        _countdownState.value = CountdownState.Idle
    }
}
