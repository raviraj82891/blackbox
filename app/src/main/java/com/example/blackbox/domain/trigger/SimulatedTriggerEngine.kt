package com.example.blackbox.domain.trigger

import com.example.blackbox.data.db.EventType
import com.example.blackbox.data.db.TriggerType
import com.example.blackbox.data.repository.BlackboxRepository
import com.example.blackbox.domain.fusion.FusionEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Simulated Trigger Engine — Debug & Viva Demonstration Tool.
 * Injects synthetic sensor events simulating a high-speed vehicle impact:
 * 1. Cruising in vehicle at 65 km/h
 * 2. Sudden harsh braking / angular rotation
 * 3. Extreme impact deceleration (34.2 m/s²)
 * 4. Acoustic impact audio event (88.5 dB)
 * 5. Complete stillness post-impact
 * Followed by triggering the 30-second emergency countdown.
 */
class SimulatedTriggerEngine(
    private val repository: BlackboxRepository,
    private val triggerDetector: TriggerDetector
) {

    suspend fun injectSimulatedCrashSequence() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()

        // 1. Vehicle Cruising (30 seconds before impact)
        repository.recordSensorEvent(
            type = EventType.ACTIVITY,
            payloadJson = """{"state":"IN_VEHICLE","confidence":98}""",
            timestampMs = now - 30000
        )
        repository.recordSensorEvent(
            type = EventType.LOCATION,
            payloadJson = """{"latitude":12.971598,"longitude":77.594562,"speed":18.1,"accuracy":4.5}""",
            timestampMs = now - 25000
        )

        // 2. Harsh Braking & Rotation (5 seconds before impact)
        repository.recordSensorEvent(
            type = EventType.ACCEL,
            payloadJson = """{"x":1.2,"y":18.5,"z":4.1,"magnitude":19.0}""",
            timestampMs = now - 5000
        )
        repository.recordSensorEvent(
            type = EventType.GYRO,
            payloadJson = """{"x":0.5,"y":2.8,"z":1.9,"deltaMagnitude":3.4}""",
            timestampMs = now - 4500
        )

        // 3. Peak Impact (0 seconds)
        repository.recordSensorEvent(
            type = EventType.ACCEL,
            payloadJson = """{"x":12.4,"y":31.8,"z":6.2,"magnitude":34.7}""",
            timestampMs = now
        )
        repository.recordSensorEvent(
            type = EventType.AUDIO_EVENT,
            payloadJson = """{"eventLabel":"LOUD_IMPACT","decibels":88.5,"confidence":0.94}""",
            timestampMs = now + 200
        )

        // 4. Post-Impact Stillness
        repository.recordSensorEvent(
            type = EventType.ACTIVITY,
            payloadJson = """{"state":"STILL","confidence":100}""",
            timestampMs = now + 5000
        )

        // 5. Trigger the emergency countdown
        withContext(Dispatchers.Main) {
            triggerDetector.startCountdown(TriggerType.SIMULATED, 34.7)
        }
    }
}
