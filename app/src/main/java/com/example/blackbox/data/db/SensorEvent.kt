package com.example.blackbox.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sensor_events")
data class SensorEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestampMs: Long,
    val type: EventType,
    val payloadJson: String,
    val prevHash: String,
    val entryHash: String
)
