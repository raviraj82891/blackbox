package com.example.blackbox.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalibrationCalculatorTest {

    @Test
    fun testNormalCarryingBaselineCalculatesAccurateStats() {
        // Simulated normal walking/carrying accelerometer magnitudes (around 9.8 m/s² with small variance)
        val normalReadings = List(100) { 9.81f + (it % 5 - 2) * 0.1f }

        val result = CalibrationCalculator.calculateBaselineAndThreshold(normalReadings)

        assertTrue("Expected CalibrationResult.Success", result is CalibrationResult.Success)
        val success = result as CalibrationResult.Success

        assertEquals(100, success.sampleCount)
        assertEquals(9.81, success.meanMagnitude, 0.2)
        assertEquals(9.81, success.medianMagnitude, 0.2)
        assertTrue("Derived threshold should be between 20.0 and 26.0 m/s²", success.derivedImpactThreshold in 20.0..26.0)
    }

    @Test
    fun testFiltersInvalidGlitchesAndNaNs() {
        val glitchyReadings = mutableListOf<Float>().apply {
            addAll(List(50) { 9.81f })
            add(Float.NaN)
            add(Float.POSITIVE_INFINITY)
            add(-5.0f)
            add(150.0f) // Sensor glitch spike
        }

        val result = CalibrationCalculator.calculateBaselineAndThreshold(glitchyReadings)

        assertTrue(result is CalibrationResult.Success)
        val success = result as CalibrationResult.Success

        assertEquals(50, success.sampleCount)
        assertEquals(9.81, success.meanMagnitude, 0.1)
    }

    @Test
    fun testDetectsAbnormalShakingAndPromptsRetry() {
        // High shaking magnitudes (mean ~ 25 m/s², large variance)
        val shakingReadings = List(60) { 25.0f + (it % 10) * 2.0f }

        val result = CalibrationCalculator.calculateBaselineAndThreshold(shakingReadings)

        assertTrue("Expected AbnormalMotion result for violent shaking", result is CalibrationResult.AbnormalMotion)
        val abnormal = result as CalibrationResult.AbnormalMotion
        assertTrue(abnormal.reason.contains("Excessive shaking"))
    }

    @Test
    fun testInsufficientReadingsReturnsError() {
        val tooFewReadings = listOf(9.8f, 9.81f, 9.82f) // Only 3 readings

        val result = CalibrationCalculator.calculateBaselineAndThreshold(tooFewReadings)

        assertTrue(result is CalibrationResult.Error)
        val error = result as CalibrationResult.Error
        assertTrue(error.reason.contains("Insufficient valid accelerometer readings"))
    }
}
