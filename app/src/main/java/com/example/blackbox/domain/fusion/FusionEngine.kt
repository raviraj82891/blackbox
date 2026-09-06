package com.example.blackbox.domain.fusion

import com.example.blackbox.data.db.EventType
import com.example.blackbox.data.db.SensorEvent
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

enum class SeverityLevel {
    INFO,
    WARNING,
    CRITICAL
}

/**
 * Fusion Engine — Rule-based state machine turning raw heterogeneous readings into
 * a human-readable, chronological incident timeline.
 * Example timeline sequence:
 * "Normal Walking" -> "In Vehicle (45 km/h)" -> "Sudden Deceleration (2.8G)" -> "Impact Detected (32 m/s²)" -> "Post-Event Stillness (No Movement)"
 */
object FusionEngine {

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun reconstructTimeline(rawEvents: List<SensorEvent>): List<TimelineEntry> {
        val entries = mutableListOf<TimelineEntry>()

        for (event in rawEvents.sortedBy { it.timestampMs }) {
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
                                detailedDescription = "High peak force detected: %.2f m/s² (3-axis vector magnitude)".format(mag),
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
                    if (label == "LOUD_IMPACT") {
                        entries.add(
                            TimelineEntry(
                                timestampMs = event.timestampMs,
                                formattedTime = dateStr,
                                eventType = event.type,
                                summaryTitle = "Audio Event: Loud Acoustic Impact",
                                detailedDescription = "Peak acoustic amplitude: %.1f dB (Classified event, no raw audio stored)".format(db),
                                severityLevel = SeverityLevel.CRITICAL,
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
                                summaryTitle = "Audio Event: High Ambient Noise / Raised Voice",
                                detailedDescription = "Acoustic level: %.1f dB".format(db),
                                severityLevel = SeverityLevel.WARNING,
                                rawPayload = event.payloadJson,
                                entryHash = event.entryHash
                            )
                        )
                    }
                }
                EventType.SYSTEM_EVENT -> {
                    val sysMsg = json?.optString("message", "System Status Change") ?: "System Event"
                    entries.add(
                        TimelineEntry(
                            timestampMs = event.timestampMs,
                            formattedTime = dateStr,
                            eventType = event.type,
                            summaryTitle = sysMsg,
                            detailedDescription = "Blackbox internal system log entry",
                            severityLevel = SeverityLevel.INFO,
                            rawPayload = event.payloadJson,
                            entryHash = event.entryHash
                        )
                    )
                }
                else -> {
                    entries.add(
                        TimelineEntry(
                            timestampMs = event.timestampMs,
                            formattedTime = dateStr,
                            eventType = event.type,
                            summaryTitle = "${event.type.name} Event",
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
}
