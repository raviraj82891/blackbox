package com.example.blackbox.domain.trigger

import com.example.blackbox.data.db.TriggerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AutoDetectionSettingTest {

    private lateinit var triggerDetector: TriggerDetector

    @Before
    fun setUp() {
        triggerDetector = TriggerDetector()
        triggerDetector.resetState()
        triggerDetector.isAutoDetectionEnabled = true
        triggerDetector.impactThresholdMs2 = 20.0
        triggerDetector.gyroThresholdRad = 2.5
    }

    @Test
    fun testAutomaticDetectionEnabled_TriggersCountdownOnHighImpact() {
        val baseTime = System.currentTimeMillis()

        // Free-fall weightlessness (< 1.8 m/s² for >= 200ms)
        triggerDetector.evaluateMotion(1.0f, baseTime)
        triggerDetector.evaluateMotion(1.0f, baseTime + 50L)
        triggerDetector.evaluateMotion(1.0f, baseTime + 100L)
        triggerDetector.evaluateMotion(1.0f, baseTime + 200L)

        // Free-fall ends
        triggerDetector.evaluateMotion(8.0f, baseTime + 220L)
        triggerDetector.updateGyroscope(2.5f, baseTime + 250L)

        // Heavy impact spike (35 m/s² at 280ms)
        val triggered = triggerDetector.evaluateMotion(35.0f, baseTime + 280L)

        assertTrue("Automatic detection should trigger when enabled and impact follows free-fall", triggered)
        assertTrue("Countdown state should be ActiveCountdown", triggerDetector.countdownState.value is CountdownState.ActiveCountdown)
    }

    @Test
    fun testAutomaticDetectionDisabled_IgnoresHighImpact() {
        // Disable automatic detection
        triggerDetector.isAutoDetectionEnabled = false

        val baseTime = System.currentTimeMillis()
        triggerDetector.evaluateMotion(1.0f, baseTime)
        triggerDetector.evaluateMotion(1.0f, baseTime + 200L)
        triggerDetector.evaluateMotion(8.0f, baseTime + 220L)

        val triggered = triggerDetector.evaluateMotion(35.0f, baseTime + 280L)

        assertFalse("Automatic detection should NOT trigger when disabled by user", triggered)
        assertEquals("Countdown state should remain Idle", CountdownState.Idle, triggerDetector.countdownState.value)
    }

    @Test
    fun testManualSosAlwaysWorksEvenIfAutoDetectionDisabled() {
        // Disable automatic detection
        triggerDetector.isAutoDetectionEnabled = false

        // Trigger deliberate Manual SOS
        triggerDetector.startCountdown(TriggerType.MANUAL_SOS, durationSeconds = 3)

        val state = triggerDetector.countdownState.value
        assertTrue("Manual SOS must remain fully functional even when automatic detection is disabled", state is CountdownState.ActiveCountdown)
        assertEquals(TriggerType.MANUAL_SOS, (state as CountdownState.ActiveCountdown).triggerType)
    }
}
