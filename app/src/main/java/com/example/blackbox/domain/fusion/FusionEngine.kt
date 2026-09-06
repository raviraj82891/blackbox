package com.example.blackbox.domain.fusion

import com.example.blackbox.data.db.EventType
import com.example.blackbox.data.db.SensorEvent
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

data class TimelineEntry(
    val timestampMs: Long,
    val formattedTime: String,
    val eventType: EventType,
    val summaryTitle: String,
    val detailedDescription: String,
    val severityLevel: SeverityLevel,
    val rawPayload: String,
    val entryHash: String
)

data class TimelineSession(
    val id: String,
    val activityState: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val formattedTimeRange: String,
    val durationMinutes: Int,
    val maxSpeedKmh: Double,
    val worstSeverity: SeverityLevel,
    val entries: List<TimelineEntry>
)

enum class SeverityLevel {
    INFO,
    WARNING,
    CRITICAL
}

/**
 * Fusion Engine — Rule-based state machine turning raw heterogeneous readings into
 * a human-readable, chronological incident timeline and computing the 0-100 Incident Severity Score.
 */
object FusionEngine {

    // Feature 2.1: Named Constants for Severity Score Weights
    const val ACCEL_WEIGHT = 0.40f
    const val GYRO_WEIGHT = 0.20f
    const val AUDIO_WEIGHT = 0.20f
    const val STILLNESS_WEIGHT = 0.20f

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun calculateIncidentSeverityScore(events: List<SensorEvent>): Int {
        if (events.isEmpty()) return 0

        val maxAccelMag = events.filter { it.type == EventType.ACCEL }
            .mapNotNull { runCatching { JSONObject(it.payloadJson).optDouble("magnitude", 0.0) }.getOrNull() }
            .maxOrNull() ?: 9.81
        val accelScore = ((maxAccelMag - 9.81) / (40.0 - 9.81)).coerceIn(0.0, 1.0) * 100.0

        val maxGyroDelta = events.filter { it.type == EventType.GYRO }
            .mapNotNull { runCatching { JSONObject(it.payloadJson).optDouble("deltaMagnitude", 0.0) }.getOrNull() }
            .maxOrNull() ?: 0.0
        val gyroScore = (maxGyroDelta / 5.0).coerceIn(0.0, 1.0) * 100.0

        val hasAudioImpact = events.any { it.type == EventType.AUDIO_EVENT && it.payloadJson.contains("LOUD") }
        val audioScore = if (hasAudioImpact) 100.0 else 0.0

        val stillnessScore = 80.0 // Post-impact stillness baseline score

        val finalScore = (accelScore * ACCEL_WEIGHT) +
                (gyroScore * GYRO_WEIGHT) +
                (audioScore * AUDIO_WEIGHT) +
                (stillnessScore * STILLNESS_WEIGHT)

        return finalScore.toInt().coerceIn(0, 100)
    }

    fun reconstructTimeline(rawEvents: List<SensorEvent>): List<TimelineEntry> {
        val sortedEvents = rawEvents.sortedBy { it.timestampMs }
        val entries = mutableListOf<TimelineEntry>()

        for (event in sortedEvents) {
            val dateStr = timeFormat.format(Date(event.timestampMs))
            val json = runCatching { JSONObject(event.payloadJson) }.getOrNull()

            when (event.type) {
                EventType.ACCEL -> {
                    val mag = json?.optDouble("magnitude", 0.0) ?: 0.0
                    if (mag > 28.0) {
                        entries.add(
                            TimelineEntry(
                                timestampMs = event.timestampMs,
                                formattedTime = dateStr,
                                eventType = event.type,
                                summaryTitle = "Severe Acceleration Spike / Impact",
                                detailedDescription = "High peak force detected: %.2f m/s²".format(mag),
                                severityLevel = SeverityLevel.CRITICAL,
                                rawPayload = event.payloadJson,
                                entryHash = event.entryHash
                            )
                        )
                    } else if (mag > 18.0) {
                        entries.add(
                            TimelineEntry(
                                timestampMs = event.timestampMs,
                                formattedTime = dateStr,
                                eventType = event.type,
                                summaryTitle = "Sudden Motion / Deceleration",
                                detailedDescription = "Elevated force magnitude: %.2f m/s²".format(mag),
                                severityLevel = SeverityLevel.WARNING,
                                rawPayload = event.payloadJson,
                                entryHash = event.entryHash
                            )
                        )
                    }
                }
                EventType.GYRO -> {
                    val delta = json?.optDouble("deltaMagnitude", 0.0) ?: 0.0
                    if (delta > 2.5) {
                        entries.add(
                            TimelineEntry(
                                timestampMs = event.timestampMs,
                                formattedTime = dateStr,
                                eventType = event.type,
                                summaryTitle = "Rapid Rotation / Angular Shift",
                                detailedDescription = "Rotational delta: %.2f rad/s".format(delta),
                                severityLevel = SeverityLevel.WARNING,
                                rawPayload = event.payloadJson,
                                entryHash = event.entryHash
                            )
                        )
                    }
                }
                EventType.LOCATION -> {
                    val speed = json?.optDouble("speed", 0.0) ?: 0.0
                    val lat = json?.optDouble("latitude", 0.0) ?: 0.0
                    val lon = json?.optDouble("longitude", 0.0) ?: 0.0
                    val speedKmh = speed * 3.6
                    entries.add(
                        TimelineEntry(
                            timestampMs = event.timestampMs,
                            formattedTime = dateStr,
                            eventType = event.type,
                            summaryTitle = "Location Snapshot (%.1f km/h)".format(speedKmh),
                            detailedDescription = "Coords: %.5f, %.5f | Speed: %.1f km/h".format(lat, lon, speedKmh),
                            severityLevel = if (speedKmh > 80.0) SeverityLevel.WARNING else SeverityLevel.INFO,
                            rawPayload = event.payloadJson,
                            entryHash = event.entryHash
                        )
                    )
                }
                EventType.ACTIVITY -> {
                    val state = json?.optString("state", "UNKNOWN") ?: "UNKNOWN"
                    entries.add(
                        TimelineEntry(
                            timestampMs = event.timestampMs,
                            formattedTime = dateStr,
                            eventType = event.type,
                            summaryTitle = "Activity Transition: $state",
                            detailedDescription = "User physical activity state updated to $state",
                            severityLevel = SeverityLevel.INFO,
                            rawPayload = event.payloadJson,
                            entryHash = event.entryHash
                        )
                    )
                }
                EventType.AUDIO_EVENT -> {
                    val label = json?.optString("eventLabel", "AMBIENT") ?: "AMBIENT"
                    val db = json?.optDouble("decibels", 0.0) ?: 0.0

                    val hasCoOccurringMotionSpike = sortedEvents.any { motion ->
                        (motion.type == EventType.ACCEL || motion.type == EventType.GYRO) &&
                                abs(motion.timestampMs - event.timestampMs) <= 2000L &&
                                runCatching {
                                    val mJson = JSONObject(motion.payloadJson)
                                    mJson.optDouble("magnitude", 0.0) > 20.0 || mJson.optDouble("deltaMagnitude", 0.0) > 2.0
                                }.getOrDefault(false)
                    }

                    if (label == "LOUD_ACOUSTIC_NOISE" && hasCoOccurringMotionSpike) {
                        entries.add(
                            TimelineEntry(
                                timestampMs = event.timestampMs,
                                formattedTime = dateStr,
                                eventType = event.type,
                                summaryTitle = "Audio & Physical Impact Co-occurrence",
                                detailedDescription = "Acoustic impact (%.1f dB) co-occurred with kinetic motion spike".format(db),
                                severityLevel = SeverityLevel.CRITICAL,
                                rawPayload = event.payloadJson,
                                entryHash = event.entryHash
                            )
                        )
                    } else if (label == "LOUD_ACOUSTIC_NOISE") {
                        entries.add(
                            TimelineEntry(
                                timestampMs = event.timestampMs,
                                formattedTime = dateStr,
                                eventType = event.type,
                                summaryTitle = "Audio Event: Loud Acoustic Spike",
                                detailedDescription = "High volume detected (%.1f dB) without co-occurring kinetic impact".format(db),
                                severityLevel = SeverityLevel.WARNING,
                                rawPayload = event.payloadJson,
                                entryHash = event.entryHash
                            )
                        )
                    } else if (label == "RAISED_VOICE") {
                        entries.add(
                            TimelineEntry(
                                timestampMs = event.timestampMs,
                                formattedTime = dateStr,
                                eventType = event.type,
                                summaryTitle = "Audio Event: High Ambient Noise / Voice",
                                detailedDescription = "Acoustic amplitude: %.1f dB".format(db),
                                severityLevel = SeverityLevel.INFO,
                                rawPayload = event.payloadJson,
                                entryHash = event.entryHash
                            )
                        )
                    }
                }
                else -> {
                    entries.add(
                        TimelineEntry(
                            timestampMs = event.timestampMs,
                            formattedTime = dateStr,
                            eventType = event.type,
                            summaryTitle = "${event.type.name} Snapshot",
                            detailedDescription = event.payloadJson,
                            severityLevel = SeverityLevel.INFO,
                            rawPayload = event.payloadJson,
                            entryHash = event.entryHash
                        )
                    )
                }
            }
        }
        return entries
    }

    fun groupTimelineIntoSessions(entries: List<TimelineEntry>): List<TimelineSession> {
        if (entries.isEmpty()) return emptyList()

        val sorted = entries.sortedBy { it.timestampMs }
        val sessions = mutableListOf<TimelineSession>()

        var currentActivity = "General Activity"
        var sessionStartTime = sorted.first().timestampMs
        var currentSessionEntries = mutableListOf<TimelineEntry>()

        for (entry in sorted) {
            if (entry.eventType == EventType.ACTIVITY && entry.summaryTitle.startsWith("Activity Transition")) {
                if (currentSessionEntries.isNotEmpty()) {
                    val sessionEndTime = entry.timestampMs
                    sessions.add(
                        createSessionObject(
                            activityState = currentActivity,
                            startTimeMs = sessionStartTime,
                            endTimeMs = sessionEndTime,
                            entries = currentSessionEntries.toList()
                        )
                    )
                }
                currentActivity = entry.summaryTitle.substringAfter(": ").trim()
                sessionStartTime = entry.timestampMs
                currentSessionEntries = mutableListOf()
            }
            currentSessionEntries.add(entry)
        }

        if (currentSessionEntries.isNotEmpty()) {
            val sessionEndTime = sorted.last().timestampMs
            sessions.add(
                createSessionObject(
                    activityState = currentActivity,
                    startTimeMs = sessionStartTime,
                    endTimeMs = sessionEndTime,
                    entries = currentSessionEntries.toList()
                )
            )
        }

        return sessions.reversed()
    }

    private fun createSessionObject(
        activityState: String,
        startTimeMs: Long,
        endTimeMs: Long,
        entries: List<TimelineEntry>
    ): TimelineSession {
        val startStr = timeFormat.format(Date(startTimeMs))
        val endStr = timeFormat.format(Date(endTimeMs))
        val durationMins = (((endTimeMs - startTimeMs) / 1000) / 60).toInt().coerceAtLeast(1)

        val worstSeverity = when {
            entries.any { it.severityLevel == SeverityLevel.CRITICAL } -> SeverityLevel.CRITICAL
            entries.any { it.severityLevel == SeverityLevel.WARNING } -> SeverityLevel.WARNING
            else -> SeverityLevel.INFO
        }

        val maxSpeed = entries
            .filter { it.eventType == EventType.LOCATION }
            .mapNotNull {
                runCatching { JSONObject(it.rawPayload).optDouble("speed", 0.0) * 3.6 }.getOrNull()
            }
            .maxOrNull() ?: 0.0

        return TimelineSession(
            id = "$startTimeMs-$endTimeMs",
            activityState = activityState,
            startTimeMs = startTimeMs,
            endTimeMs = endTimeMs,
            formattedTimeRange = "$startStr – $endStr",
            durationMinutes = durationMins,
            maxSpeedKmh = maxSpeed,
            worstSeverity = worstSeverity,
            entries = entries
        )
    }
}
