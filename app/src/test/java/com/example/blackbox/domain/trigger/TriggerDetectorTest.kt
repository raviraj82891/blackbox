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
        triggerDetector.impactThresholdMs2 = 20.0
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
    fun testRunningMotionDoesNotTrigger() {
        val baseTime = 1700000000000L
        val runningMagnitudes = listOf(9.8f, 12.8f, 7.2f, 13.5f, 6.5f, 14.1f, 8.0f)

        for ((index, mag) in runningMagnitudes.withIndex()) {
            val triggered = triggerDetector.evaluateMotion(mag, baseTime + (index * 150L))
            assertFalse("Dynamic running motion must not trigger detection", triggered)
        }
        assertEquals(CountdownState.Idle, triggerDetector.countdownState.value)
    }

    @Test
    fun testVehicleVibrationDoesNotTrigger() {
        val baseTime = 1700000000000L
        val vehicleMagnitudes = listOf(9.8f, 10.9f, 11.4f, 10.1f, 11.8f, 9.2f)

        for ((index, mag) in vehicleMagnitudes.withIndex()) {
            val triggered = triggerDetector.evaluateMotion(mag, baseTime + (index * 100L))
            assertFalse("Vehicle vibration must not trigger crash detection", triggered)
        }
        assertEquals(CountdownState.Idle, triggerDetector.countdownState.value)
    }

    @Test
    fun testHardTableImpactWithoutGyroRotationIsFilteredOut() {
        val baseTime = 1700000000000L

        // Gyro remains low (0.5 rad/s < threshold 2.5 rad/s)
        triggerDetector.updateGyroscope(0.5f, baseTime)

        // Single high accelerometer spike (25 m/s² > 20 m/s²) without gyro rotation or free-fall
        val triggered = triggerDetector.evaluateMotion(25.0f, baseTime)

        assertFalse("Hard table drop / phone bump without gyro rotation or free-fall must be filtered out", triggered)
        assertEquals(CountdownState.Idle, triggerDetector.countdownState.value)
    }

    @Test
    fun testHighGyroRotationAloneWithoutImpactDoesNotTrigger() {
        val baseTime = 1700000000000L

        // High rotation (5.0 rad/s > threshold 2.5 rad/s)
        triggerDetector.updateGyroscope(5.0f, baseTime)

        // Normal gravity accelerometer reading (9.81 m/s²)
        val triggered = triggerDetector.evaluateMotion(9.81f, baseTime)

        assertFalse("High gyroscopic rotation alone without impact must not trigger crash detection", triggered)
        assertEquals(CountdownState.Idle, triggerDetector.countdownState.value)
    }

    @Test
    fun testGenuineCrashWithImpactAndGyroRotationTriggersAutoCrash() {
        val baseTime = 1700000000000L

        // Stage 3: Gyroscope registers angular rotation spike (3.5 rad/s >= threshold 2.5 rad/s)
        triggerDetector.updateGyroscope(3.5f, baseTime)

        // Stage 2: Heavy impact spike (32 m/s² > threshold 20 m/s²)
        val triggered = triggerDetector.evaluateMotion(32.0f, baseTime)

        assertTrue("Genuine crash with high impact and gyro rotation must trigger AUTO_CRASH", triggered)

        val state = triggerDetector.countdownState.value
        assertTrue("State must be ActiveCountdown", state is CountdownState.ActiveCountdown)
        val active = state as CountdownState.ActiveCountdown
        assertEquals(TriggerType.AUTO_CRASH, active.triggerType)
        assertEquals(30, active.secondsRemaining)
        assertEquals(32.0, active.impactMagnitude, 0.01)
    }

    @Test
    fun testGenuineFallWithFreeFallAndImpactTriggersAutoFall() {
        val baseTime = 1700000000000L

        // Stage 1: Free-fall weightlessness (< 3.5 m/s² for 300ms)
        triggerDetector.evaluateMotion(1.2f, baseTime)
        triggerDetector.evaluateMotion(1.0f, baseTime + 200L)
        triggerDetector.evaluateMotion(1.1f, baseTime + 300L)

        // Stage 2: Heavy impact spike (24 m/s² > threshold 20 m/s²)
        val triggered = triggerDetector.evaluateMotion(24.0f, baseTime + 400L)

        assertTrue("Genuine fall with free-fall and impact must trigger AUTO_FALL", triggered)

        val state = triggerDetector.countdownState.value
        assertTrue("State must be ActiveCountdown", state is CountdownState.ActiveCountdown)
        val active = state as CountdownState.ActiveCountdown
        assertEquals(TriggerType.AUTO_FALL, active.triggerType)
        assertEquals(30, active.secondsRemaining)
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
        triggerDetector.updateGyroscope(3.0f, baseTime)
        val firstTrigger = triggerDetector.evaluateMotion(30.0f, baseTime)
        assertTrue("First impact must trigger countdown", firstTrigger)

        // Subsequent impact while countdown is active
        val secondTrigger = triggerDetector.evaluateMotion(40.0f, baseTime + 1000L)
        assertFalse("Subsequent impact while countdown is active must be ignored to prevent duplicate triggers", secondTrigger)
    }

    @Test
    fun testDetectorStateReset() {
        val baseTime = 1700000000000L

        triggerDetector.updateGyroscope(4.0f, baseTime)
        triggerDetector.evaluateMotion(35.0f, baseTime)

        // Cancel countdown and reset state
        triggerDetector.cancelCountdown()
        assertEquals(CountdownState.Idle, triggerDetector.countdownState.value)

        // Reset state clears internal free-fall and gyro parameters
        triggerDetector.resetState()
        assertEquals(CountdownState.Idle, triggerDetector.countdownState.value)
    }
}
