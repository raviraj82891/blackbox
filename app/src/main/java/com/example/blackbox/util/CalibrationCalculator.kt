package com.example.blackbox.util

import kotlin.math.sqrt

sealed class CalibrationResult {
    data class Success(
        val sampleCount: Int,
        val meanMagnitude: Double,
        val medianMagnitude: Double,
        val stdDevMagnitude: Double,
        val p95Spread: Double,
        val derivedImpactThreshold: Double,
        val timestampMs: Long = System.currentTimeMillis()
    ) : CalibrationResult()

    data class AbnormalMotion(
        val reason: String,
        val meanMagnitude: Double,
        val stdDevMagnitude: Double
    ) : CalibrationResult()

    data class Error(val reason: String) : CalibrationResult()
}

/**
 * Personal Calibration Calculator — Statistical engine that analyzes raw accelerometer
 * readings collected over a 10-second carrying window to derive a personalized crash impact threshold.
 */
object CalibrationCalculator {

    /**
     * Analyzes raw accelerometer magnitude readings and derives personalized baseline stats and threshold.
     *
     * @param rawMagnitudes List of accelerometer vector magnitude floats collected during calibration.
     * @return [CalibrationResult] representing Success with derived stats, AbnormalMotion, or Error.
     */
    fun calculateBaselineAndThreshold(rawMagnitudes: List<Float>): CalibrationResult {
        // Step 1: Filter out invalid sensor readings (glitches, NaNs, <= 0)
        val validReadings = rawMagnitudes
            .filter { !it.isNaN() && !it.isInfinite() && it > 0.1f && it < 100.0f }
            .map { it.toDouble() }

        // Require at least 30 valid sensor readings (approx 0.6 seconds at 50Hz)
        if (validReadings.size < 30) {
            return CalibrationResult.Error("Insufficient valid accelerometer readings collected (${validReadings.size}/30 required).")
        }

        val count = validReadings.size
        val mean = validReadings.sum() / count

        val sorted = validReadings.sorted()
        val median = if (count % 2 == 0) {
            (sorted[count / 2 - 1] + sorted[count / 2]) / 2.0
        } else {
            sorted[count / 2]
        }

        // Variance & Standard Deviation
        val variance = validReadings.sumOf { (it - mean) * (it - mean) } / (count - 1).coerceAtLeast(1)
        val stdDev = sqrt(variance)

        // 95th Percentile Spread
        val p95Index = ((count - 1) * 0.95).toInt().coerceIn(0, count - 1)
        val p95Spread = sorted[p95Index]

        // Step 2: Detect Abnormal / Excessive Motion during calibration
        if (mean > 18.0 || stdDev > 4.5) {
            return CalibrationResult.AbnormalMotion(
                reason = "Excessive shaking or movement detected during calibration (mean: %.1f m/s², stdDev: %.1f). Please keep phone still or carry normally and retry.".format(mean, stdDev),
                meanMagnitude = mean,
                stdDevMagnitude = stdDev
            )
        }

        // Step 3: Documented Impact Threshold Algorithm:
        // Derived Threshold = Mean Baseline + (3.5 * StdDev) + 12.0 m/s² Safety Margin
        // Bounded strictly between 18.0 m/s² (sensitive) and 38.0 m/s² (high-g crash)
        val rawDerived = mean + (3.5 * stdDev) + 12.0
        val derivedThreshold = rawDerived.coerceIn(18.0, 38.0)

        return CalibrationResult.Success(
            sampleCount = count,
            meanMagnitude = mean,
            medianMagnitude = median,
            stdDevMagnitude = stdDev,
            p95Spread = p95Spread,
            derivedImpactThreshold = derivedThreshold
        )
    }
}
