package com.example.blackbox.domain.trigger

import com.example.blackbox.data.db.TriggerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TriggerDetectorTest {

    private lateinit var triggerDetector: TriggerDetector

    @Before
    fun setUp() {
        triggerDetector = TriggerDetector()
        triggerDetector.resetState()
        triggerDetector.impactThresholdMs2 = 24.0
        triggerDetector.gyroThresholdRad = 2.5
        triggerDetector.isAutoDetectionEnabled = true
    }

    @Test
    fun testNormalWalkingDoesNotTrigger() {
        val baseTime = 1700000000000L
        val walkingMagnitudes = listOf(9.8f, 10.2f, 11.1f, 9.5f, 10.0f, 8.9f, 10.5f)

        for ((index, mag) in walkingMagnitudes.withIndex()) {
            val triggered = triggerDetector.evaluateMotion(mag, baseTime + (index * 200L))
            assertFalse("Normal walking motion must not trigger crash or fall detection", triggered)
        }
        assertEquals(CountdownState.Idle, triggerDetector.countdownState.value)
    }

    @Test
    fun testCandidateRejectedForInsufficientFreeFallNeverReachesCountdown() {
        val baseTime = 1700000000000L

        // 1. Brief weightless dip < 200ms (e.g. 150ms)
        triggerDetector.evaluateMotion(1.5f, baseTime)
        triggerDetector.evaluateMotion(1.5f, baseTime + 100L)

        // 2. Next sample returns above 1.8 m/s² -> Insufficient duration (150ms < 200ms) -> REJECTED & TOKEN INVALIDATED
        val rejectedResult = triggerDetector.evaluateMotion(10.0f, baseTime + 150L)
        assertFalse("Rejected candidate must return false", rejectedResult)

        // 3. High impact spike 68.0 m/s² occurs without valid candidate token
        triggerDetector.updateGyroscope(3.5f, baseTime + 180L)
        val highSpikeTriggered = triggerDetector.evaluateMotion(68.0f, baseTime + 180L)

        // MUST NOT transition to FALL_CONFIRMED, AUTO_CRASH, or EMERGENCY_COUNTDOWN!
        assertFalse("A rejected candidate must NEVER reach AUTO_CRASH or EMERGENCY_COUNTDOWN without a valid candidate token", highSpikeTriggered)
        assertEquals(CountdownState.Idle, triggerDetector.countdownState.value)
    }

    @Test
    fun testZeroDelayImpactWithoutPriorFreeFallEndIsRejected() {
        val baseTime = 1700000000000L

        // Impact spike occurs without free-fall end transition (freeFallToImpactMs == 0)
        triggerDetector.evaluateMotion(1.0f, baseTime)
        triggerDetector.evaluateMotion(1.0f, baseTime + 200L)

        // Impact on same millisecond as free-fall sample without freeFallToImpactMs delay
        val triggered = triggerDetector.evaluateMotion(28.0f, baseTime + 200L)
        assertFalse("Impact occurring with zero delay (freeFallToImpactMs == 0) must be rejected", triggered)
    }

    @Test
    fun testSevenMsFreeFallToImpactDelayIsAccepted() {
        val baseTime = 1700000000000L

        // Free-fall >= 200ms
        triggerDetector.evaluateMotion(1.0f, baseTime)
        triggerDetector.evaluateMotion(1.0f, baseTime + 200L)

        // Exits free-fall at t=200ms into a non-impact sample (5.0 m/s²)
        triggerDetector.evaluateMotion(5.0f, baseTime + 200L)

        // Gyro rotation
        triggerDetector.updateGyroscope(2.2f, baseTime + 207L)

        // Impact occurs 7ms later at t=207ms (freeFallToImpactMs = 207 - 200 = 7ms)
        val triggered = triggerDetector.evaluateMotion(28.0f, baseTime + 207L)

        assertTrue("Free-fall to impact delay of 7ms (1ms..600ms window) must be accepted", triggered)
        val state = triggerDetector.countdownState.value
        assertTrue(state is CountdownState.ActiveCountdown)
        assertEquals(TriggerType.AUTO_FALL, (state as CountdownState.ActiveCountdown).triggerType)
    }

    @Test
    fun testSixHundredOneMsFreeFallToImpactDelayIsRejected() {
        val baseTime = 1700000000000L

        // Free-fall >= 200ms
        triggerDetector.evaluateMotion(1.0f, baseTime)
        triggerDetector.evaluateMotion(1.0f, baseTime + 200L)

        // Exits free-fall at t=200ms
        triggerDetector.evaluateMotion(5.0f, baseTime + 200L)

        // Impact occurs 601ms later at t=801ms (> 600ms max allowed window)
        triggerDetector.updateGyroscope(2.2f, baseTime + 801L)
        val triggered = triggerDetector.evaluateMotion(28.0f, baseTime + 801L)

        assertFalse("Free-fall to impact delay of 601ms (> 600ms max allowed) must be rejected", triggered)
        assertEquals(CountdownState.Idle, triggerDetector.countdownState.value)
    }

    @Test
    fun testDirectTransitionToImpactSampleUsesLastWeightlessTimestampBoundary() {
        val baseTime = 1700000000000L

        // Stage 1: Free-fall weightlessness (< 1.8 m/s² for >= 200ms)
        triggerDetector.evaluateMotion(1.2f, baseTime)
        triggerDetector.evaluateMotion(1.0f, baseTime + 50L)
        triggerDetector.evaluateMotion(1.1f, baseTime + 100L)
        triggerDetector.evaluateMotion(0.8f, baseTime + 200L) // Last weightless sample at t=200ms

        // Next sample at t=220ms is ALREADY an impact spike (28.0 m/s² >= 24.0 m/s²)
        // Gyro rotation registered at t=220ms
        triggerDetector.updateGyroscope(2.2f, baseTime + 220L)
        val triggered = triggerDetector.evaluateMotion(28.0f, baseTime + 220L)

        // freeFallEndTimestampMs is set to lastWeightlessTimestampMs (200ms)
        // impactTimestampMs = 220ms
        // freeFallToImpactMs = 220 - 200 = 20ms (> 0ms, valid delay in 1..600ms!)
        assertTrue("Direct transition from weightlessness to impact sample must use last weightless timestamp boundary and trigger AUTO_FALL", triggered)

        val state = triggerDetector.countdownState.value
        assertTrue(state is CountdownState.ActiveCountdown)
        assertEquals(TriggerType.AUTO_FALL, (state as CountdownState.ActiveCountdown).triggerType)
    }

    @Test
    fun testPreImpactGyroIgnoredAndRejectsFall() {
        val baseTime = 1700000000000L

        // Pre-impact gyro timestamp from 500ms before free-fall
        triggerDetector.updateGyroscope(3.0f, baseTime - 500L)

        // Free-fall
        triggerDetector.evaluateMotion(1.0f, baseTime)
        triggerDetector.evaluateMotion(1.0f, baseTime + 200L)
        triggerDetector.evaluateMotion(8.0f, baseTime + 220L)

        // Impact 25.0 m/s² without post-impact gyro
        val triggered = triggerDetector.evaluateMotion(25.0f, baseTime + 300L)

        assertFalse("Pre-impact gyro spike must be ignored and not confirm post-impact fall", triggered)
        assertEquals(CountdownState.Idle, triggerDetector.countdownState.value)
    }

    @Test
    fun testStaleGyroIgnoredAndRejectsFall() {
        val baseTime = 1700000000000L

        // Free-fall
        triggerDetector.evaluateMotion(1.0f, baseTime)
        triggerDetector.evaluateMotion(1.0f, baseTime + 200L)
        triggerDetector.evaluateMotion(8.0f, baseTime + 220L)

        // Stale gyro reading from 5 seconds ago
        triggerDetector.updateGyroscope(3.5f, baseTime - 5000L)

        // Impact 25.0 m/s²
        val triggered = triggerDetector.evaluateMotion(25.0f, baseTime + 300L)

        assertFalse("Stale gyro reading from 5s ago must be ignored and not confirm post-impact fall", triggered)
        assertEquals(CountdownState.Idle, triggerDetector.countdownState.value)
    }

    @Test
    fun testValidHighImpactConfirmationWithoutGyro() {
        val baseTime = 1700000000000L

        // Free-fall
        triggerDetector.evaluateMotion(1.0f, baseTime)
        triggerDetector.evaluateMotion(1.0f, baseTime + 200L)
        triggerDetector.evaluateMotion(8.0f, baseTime + 220L)

        // High impact force 34.0 m/s² (>= 32.0 m/s² high kinetic impact shift threshold)
        val triggered = triggerDetector.evaluateMotion(34.0f, baseTime + 300L)

        assertTrue("High kinetic impact shift >= 32 m/s² confirms fall even if gyro is low", triggered)
        val state = triggerDetector.countdownState.value
        assertTrue(state is CountdownState.ActiveCountdown)
        assertEquals(TriggerType.AUTO_FALL, (state as CountdownState.ActiveCountdown).triggerType)
    }

    @Test
    fun testGenuineFallWithSustainedFreeFallAndImpactTriggersAutoFall() {
        val baseTime = 1700000000000L

        // Stage 1: Free-fall weightlessness (< 1.8 m/s² for >= 200ms)
        triggerDetector.evaluateMotion(1.2f, baseTime)
        triggerDetector.evaluateMotion(1.0f, baseTime + 50L)
        triggerDetector.evaluateMotion(1.1f, baseTime + 100L)
        triggerDetector.evaluateMotion(0.8f, baseTime + 200L) // Validated free-fall (200ms)

        // Free-fall ends
        triggerDetector.evaluateMotion(8.0f, baseTime + 220L)

        // Gyro rotation during fall
        triggerDetector.updateGyroscope(2.2f, baseTime + 250L)

        // Stage 2: Heavy impact spike (28.0 m/s² >= threshold 24.0 m/s²) within 600ms after free-fall
        val triggered = triggerDetector.evaluateMotion(28.0f, baseTime + 340L)

        assertTrue("Genuine fall with sustained free-fall and impact delay must trigger AUTO_FALL", triggered)

        val state = triggerDetector.countdownState.value
        assertTrue(state is CountdownState.ActiveCountdown)
        assertEquals(TriggerType.AUTO_FALL, (state as CountdownState.ActiveCountdown).triggerType)
        assertEquals(30, (state as CountdownState.ActiveCountdown).secondsRemaining)
    }

    @Test
    fun testManualSosIsIndependent3SecondPath() {
        triggerDetector.startCountdown(TriggerType.MANUAL_SOS, durationSeconds = 3)

        val state = triggerDetector.countdownState.value
        assertTrue("Manual SOS must trigger 3-second ActiveCountdown", state is CountdownState.ActiveCountdown)
        val active = state as CountdownState.ActiveCountdown
        assertEquals(TriggerType.MANUAL_SOS, active.triggerType)
        assertEquals(3, active.secondsRemaining)
    }

    @Test
    fun testDuplicateTriggerPreventionWhileCountdownActive() {
        val baseTime = 1700000000000L

        // First trigger
        triggerDetector.evaluateMotion(1.0f, baseTime)
        triggerDetector.evaluateMotion(1.0f, baseTime + 200L)
        triggerDetector.evaluateMotion(8.0f, baseTime + 220L)
        triggerDetector.updateGyroscope(2.2f, baseTime + 250L)
        val firstTrigger = triggerDetector.evaluateMotion(28.0f, baseTime + 340L)
        assertTrue("First impact must trigger countdown", firstTrigger)

        // Subsequent impact while countdown is active
        val secondTrigger = triggerDetector.evaluateMotion(40.0f, baseTime + 1000L)
        assertFalse("Subsequent impact while countdown is active must be ignored to prevent duplicate triggers", secondTrigger)
    }

    @Test
    fun testDetectorStateReset() {
        val baseTime = 1700000000000L

        triggerDetector.evaluateMotion(1.0f, baseTime)
        triggerDetector.evaluateMotion(1.0f, baseTime + 200L)

        // Cancel countdown and reset state
        triggerDetector.cancelCountdown()
        assertEquals(CountdownState.Idle, triggerDetector.countdownState.value)

        // Reset state clears internal free-fall and gyro parameters
        triggerDetector.resetState()
        assertEquals(CountdownState.Idle, triggerDetector.countdownState.value)
    }
}
