package com.example.blackbox.domain.trigger

import android.util.Log
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

enum class DetectorPhysicsState {
    NORMAL,
    FREE_FALL_CANDIDATE,
    FREE_FALL_VALIDATED,
    IMPACT_DETECTED,
    FALL_CONFIRMED
}

/**
 * Immutable Fall Candidate Data Class tracking explicit, non-derived timestamps
 * for free-fall start, last weightless sample, free-fall end, and impact events.
 */
private data class FallCandidate(
    val token: Long,
    val freeFallStartTimestampMs: Long,
    var lastWeightlessTimestampMs: Long = 0L,
    var freeFallEndTimestampMs: Long = 0L,
    var impactTimestampMs: Long = 0L,
    var isValidFreeFall: Boolean = false,
    var isImpactDetected: Boolean = false
) {
    val freeFallDurationMs: Long
        get() = if (freeFallStartTimestampMs > 0L) {
            val end = if (freeFallEndTimestampMs > 0L) freeFallEndTimestampMs else lastWeightlessTimestampMs
            if (end >= freeFallStartTimestampMs) end - freeFallStartTimestampMs else 0L
        } else 0L

    val freeFallToImpactMs: Long
        get() = if (freeFallEndTimestampMs > 0L && impactTimestampMs > freeFallEndTimestampMs) {
            impactTimestampMs - freeFallEndTimestampMs
        } else 0L
}

/**
 * Trigger Detector — Single-Owner Thread-Safe On-Device Physics Engine.
 * Resolves free-fall/impact timestamp collision when weightlessness exits directly into an impact spike.
 *
 * Specificity Physics Rules:
 * 1. Free-Fall Weightlessness: Vector magnitude < 1.8 m/s² (0.18g) sustained for >= 200ms (10+ samples at 50Hz).
 * 2. High Impact Spike: Vector magnitude >= 24.0 m/s² (2.45g) occurring within 1ms..600ms AFTER free-fall boundary.
 * 3. Temporally Valid Gyro Rotation: Angular rotation delta >= 2.0 rad/s with timestamp in [-100ms..+600ms] of impact.
 */
class TriggerDetector {

    var impactThresholdMs2: Double = 24.0
    var gyroThresholdRad: Double = 2.5
    var postImpactStillnessWindowMs: Long = 20000L

    private val _countdownState = MutableStateFlow<CountdownState>(CountdownState.Idle)
    val countdownState: StateFlow<CountdownState> = _countdownState.asStateFlow()

    private var physicsState: DetectorPhysicsState = DetectorPhysicsState.NORMAL

    private var lastPeakMagnitude: Double = 0.0

    // Latest gyroscopic rotation magnitude (rad/s) & timestamp
    private var latestGyroDelta: Float = 0f
    private var latestGyroTimestampMs: Long = 0L

    // Explicit Fall Candidate Instance
    private var activeCandidate: FallCandidate? = null

    // Adaptive sensitivity calibration counter
    private var cancelledCountdownsInWindow = 0
    private var lastCancelledPeakMagnitude = 0.0

    // Master toggle for automatic crash/fall evaluation
    var isAutoDetectionEnabled: Boolean = true

    @Synchronized
    fun updateGyroscope(delta: Float, timestampMs: Long = System.currentTimeMillis()) {
        latestGyroDelta = delta
        latestGyroTimestampMs = timestampMs
    }

    /**
     * Single-owner, thread-serialized motion evaluator with boundary timestamp collision resolution.
     * Enforces the complete ordered sequence:
     * CANDIDATE_STARTED -> sustained FREE_FALL (>=200ms) -> FREE_FALL_VALIDATED ->
     * IMPACT_VALID (1ms..600ms delay) -> Temporally Valid Post-Impact Confirmation -> AUTO_FALL.
     */
    @Synchronized
    fun evaluateMotion(accelMagnitude: Float, timestampMs: Long): Boolean {
        if (!isAutoDetectionEnabled) return false

        // Prevent duplicate triggers while a countdown or activation is already active
        if (_countdownState.value is CountdownState.ActiveCountdown || _countdownState.value is CountdownState.Activated) {
            return false
        }

        // 1. STAGE 1: Free-Fall Weightlessness Evaluation (< 1.8 m/s² / 0.18g)
        if (accelMagnitude < 1.8f) {
            val candidate = activeCandidate ?: FallCandidate(
                token = timestampMs.coerceAtLeast(1L),
                freeFallStartTimestampMs = timestampMs
            ).also {
                activeCandidate = it
                physicsState = DetectorPhysicsState.FREE_FALL_CANDIDATE
                Log.d("TRACE_FALL_DEBUG", "CANDIDATE_STARTED token=${it.token} timestamp=$timestampMs accel=$accelMagnitude")
            }

            candidate.lastWeightlessTimestampMs = timestampMs

            val currentDuration = timestampMs - candidate.freeFallStartTimestampMs
            if (!candidate.isValidFreeFall && currentDuration >= 200L) {
                candidate.isValidFreeFall = true
                physicsState = DetectorPhysicsState.FREE_FALL_VALIDATED
                Log.d("TRACE_FALL_DEBUG", "FREE_FALL_VALID token=${candidate.token} durationMs=$currentDuration accel=$accelMagnitude")
            }
        } else {
            // Acceleration returned above 1.8 m/s²
            val candidate = activeCandidate
            if (candidate != null && candidate.freeFallEndTimestampMs == 0L) {
                // If current sample is NOT an impact sample (< 24.0 m/s²), use current timestamp.
                // If current sample IS an impact sample (>= 24.0 m/s²), use last weightless sample timestamp as the free-fall boundary.
                if (accelMagnitude < impactThresholdMs2.toFloat()) {
                    candidate.freeFallEndTimestampMs = timestampMs
                } else {
                    candidate.freeFallEndTimestampMs = candidate.lastWeightlessTimestampMs
                }

                val freeFallDuration = candidate.freeFallDurationMs

                if (!candidate.isValidFreeFall || freeFallDuration < 200L) {
                    // Candidate failed to sustain 200ms of weightlessness -> REJECT IMMEDIATELY
                    logTriggerDecision(
                        triggerType = "NONE",
                        accepted = false,
                        reason = "No valid sustained free-fall (duration ${freeFallDuration}ms < 200ms)",
                        candidate = candidate,
                        impactMag = accelMagnitude,
                        postImpactEvidence = "NONE"
                    )
                    invalidateCandidate("Insufficient free-fall duration (${freeFallDuration}ms < 200ms)")
                    return false
                }
            } else if (candidate != null && candidate.isValidFreeFall) {
                // Check if free-fall window expired (> 600ms after free-fall ended without impact)
                val delaySinceEnd = timestampMs - candidate.freeFallEndTimestampMs
                if (delaySinceEnd > MAX_FREE_FALL_TO_IMPACT_MS && accelMagnitude in 4.0f..15.0f) {
                    logTriggerDecision(
                        triggerType = "NONE",
                        accepted = false,
                        reason = "Free-fall to impact delay timeout (${delaySinceEnd}ms > 600ms)",
                        candidate = candidate,
                        impactMag = accelMagnitude,
                        postImpactEvidence = "NONE"
                    )
                    invalidateCandidate("Free-fall window timeout (${delaySinceEnd}ms > 600ms)")
                    return false
                }
            }
        }

        // 2. STAGE 2: Impact Spike Evaluation (>= 24.0 m/s²)
        if (accelMagnitude >= impactThresholdMs2.toFloat()) {
            lastPeakMagnitude = accelMagnitude.toDouble()
            val candidate = activeCandidate

            // Strict Validation: A valid candidate token with confirmed 200ms+ free-fall is MANDATORY
            if (candidate == null || !candidate.isValidFreeFall) {
                logTriggerDecision(
                    triggerType = "NONE",
                    accepted = false,
                    reason = "No valid free-fall candidate (Spike $accelMagnitude m/s² ignored)",
                    candidate = candidate ?: FallCandidate(0L, 0L),
                    impactMag = accelMagnitude,
                    postImpactEvidence = "NONE"
                )
                invalidateCandidate("No valid free-fall candidate token for impact spike")
                return false
            }

            candidate.impactTimestampMs = timestampMs
            val freeFallToImpactMs = candidate.freeFallToImpactMs

            // Impact MUST occur AFTER free-fall boundary with a valid delay (1ms..600ms)
            val isPlausibleFallDelay = freeFallToImpactMs in MIN_FREE_FALL_TO_IMPACT_MS..MAX_FREE_FALL_TO_IMPACT_MS

            if (isPlausibleFallDelay) {
                candidate.isImpactDetected = true
                physicsState = DetectorPhysicsState.IMPACT_DETECTED
                Log.d("TRACE_FALL_DEBUG", "IMPACT_VALID token=${candidate.token} magnitude=$accelMagnitude freeFallToImpactMs=$freeFallToImpactMs")

                // Stage 3: Temporally Valid Post-Impact Confirmation
                val gyroAgeRelativeToImpactMs = if (latestGyroTimestampMs > 0L) latestGyroTimestampMs - candidate.impactTimestampMs else -99999L
                val isGyroInPostImpactWindow = gyroAgeRelativeToImpactMs in -100L..600L
                val isGyroDeltaSufficient = latestGyroDelta >= 2.0f
                val isTemporallyValidPostImpactGyro = isGyroInPostImpactWindow && isGyroDeltaSufficient

                if (!isGyroInPostImpactWindow && latestGyroTimestampMs > 0L) {
                    if (latestGyroTimestampMs < candidate.impactTimestampMs - 100L) {
                        Log.d("TRACE_FALL_DEBUG", "PRE_IMPACT_GYRO_IGNORED gyroAgeRelativeToImpactMs=$gyroAgeRelativeToImpactMs gyroDelta=$latestGyroDelta")
                    } else if (latestGyroTimestampMs > candidate.impactTimestampMs + 600L) {
                        Log.d("TRACE_FALL_DEBUG", "STALE_GYRO_IGNORED gyroAgeRelativeToImpactMs=$gyroAgeRelativeToImpactMs gyroDelta=$latestGyroDelta")
                    }
                } else if (isTemporallyValidPostImpactGyro) {
                    Log.d("TRACE_FALL_DEBUG", "POST_IMPACT_GYRO_VALID gyroAgeRelativeToImpactMs=$gyroAgeRelativeToImpactMs gyroDelta=$latestGyroDelta")
                }

                val isHighImpactShift = accelMagnitude >= 32.0f
                val postImpactEvidenceText = when {
                    isTemporallyValidPostImpactGyro -> "VALID_POST_IMPACT_GYRO_ROTATION"
                    isHighImpactShift -> "HIGH_KINETIC_IMPACT_SHIFT"
                    else -> "NONE"
                }

                val confirmationOk = isTemporallyValidPostImpactGyro || isHighImpactShift

                if (confirmationOk) {
                    physicsState = DetectorPhysicsState.FALL_CONFIRMED
                    Log.d("TRACE_FALL_DEBUG", "CONFIRMATION_VALID token=${candidate.token}")

                    logTriggerDecision(
                        triggerType = "AUTO_FALL",
                        accepted = true,
                        reason = "SUCCESS",
                        candidate = candidate,
                        impactMag = accelMagnitude,
                        postImpactEvidence = postImpactEvidenceText
                    )

                    val confirmedToken = candidate.token
                    invalidateCandidate("Fall confirmed successfully for token $confirmedToken")

                    startCountdown(TriggerType.AUTO_FALL, lastPeakMagnitude, durationSeconds = 30)
                    return true
                } else {
                    logTriggerDecision(
                        triggerType = "NONE",
                        accepted = false,
                        reason = "Insufficient post-impact rotation or kinetic shift (gyro ${latestGyroDelta} rad/s)",
                        candidate = candidate,
                        impactMag = accelMagnitude,
                        postImpactEvidence = "NONE"
                    )
                    invalidateCandidate("Post-impact confirmation rejected")
                    return false
                }
            } else {
                logTriggerDecision(
                    triggerType = "NONE",
                    accepted = false,
                    reason = "Free-fall to impact delay invalid (${freeFallToImpactMs}ms not in 1..600ms)",
                    candidate = candidate,
                    impactMag = accelMagnitude,
                    postImpactEvidence = "NONE"
                )
                invalidateCandidate("Invalid free-fall to impact delay (${freeFallToImpactMs}ms)")
                return false
            }
        }
        return false
    }

    private fun logTriggerDecision(
        triggerType: String,
        accepted: Boolean,
        reason: String,
        candidate: FallCandidate,
        impactMag: Float,
        postImpactEvidence: String
    ) {
        Log.d(
            "TRACE_FALL_DEBUG",
            "TRIGGER_DECISION triggerType=$triggerType candidateToken=${candidate.token} freeFallStartTimestamp=${candidate.freeFallStartTimestampMs} freeFallEndTimestamp=${candidate.freeFallEndTimestampMs} impactTimestamp=${candidate.impactTimestampMs} freeFallDurationMs=${candidate.freeFallDurationMs}ms freeFallToImpactMs=${candidate.freeFallToImpactMs}ms impactMagnitude=$impactMag gyroEvidence=${latestGyroDelta}rad/s postImpactEvidence=$postImpactEvidence confirmationResult=${if (accepted) "SUCCESS" else "REJECTED"} decisionAccepted=$accepted rejectionReason=\"$reason\""
        )
    }

    @Synchronized
    fun startCountdown(triggerType: TriggerType, magnitude: Double = 0.0, durationSeconds: Int = 30) {
        val duration = if (triggerType == TriggerType.MANUAL_SOS) 3 else durationSeconds
        _countdownState.value = CountdownState.ActiveCountdown(
            secondsRemaining = duration,
            triggerType = triggerType,
            impactMagnitude = magnitude
        )
    }

    @Synchronized
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

    @Synchronized
    fun cancelCountdown() {
        val current = _countdownState.value
        if (current is CountdownState.ActiveCountdown && current.triggerType != TriggerType.MANUAL_SOS) {
            cancelledCountdownsInWindow++
            lastCancelledPeakMagnitude = current.impactMagnitude
        }
        invalidateCandidate("Countdown cancelled by user")
        _countdownState.value = CountdownState.Cancelled
        _countdownState.value = CountdownState.Idle
    }

    fun shouldSuggestThresholdAdjustment(): Boolean = cancelledCountdownsInWindow >= 3

    fun getSuggestedNewThreshold(): Double = (lastCancelledPeakMagnitude + 2.0).coerceAtLeast(34.0)

    fun resetAdaptiveThresholdCounter() {
        cancelledCountdownsInWindow = 0
    }

    @Synchronized
    private fun invalidateCandidate(reason: String) {
        val candidate = activeCandidate
        if (candidate != null && candidate.token != 0L) {
            Log.d("TRACE_FALL_DEBUG", "CANDIDATE_INVALIDATED token=${candidate.token} reason=\"$reason\"")
        }
        activeCandidate = null
        physicsState = DetectorPhysicsState.NORMAL
    }

    @Synchronized
    fun resetState() {
        invalidateCandidate("Global detector reset")
        latestGyroDelta = 0f
        latestGyroTimestampMs = 0L
        _countdownState.value = CountdownState.Idle
    }

    companion object {
        private const val MIN_FREE_FALL_TO_IMPACT_MS = 1L
        private const val MAX_FREE_FALL_TO_IMPACT_MS = 600L
    }
}
