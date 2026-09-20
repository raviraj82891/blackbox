package com.example.blackbox.data.db

import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun fromEventType(value: EventType): String = value.name

    @TypeConverter
    fun toEventType(value: String): EventType = runCatching { EventType.valueOf(value) }.getOrDefault(EventType.ACCEL)

    @TypeConverter
    fun fromTriggerType(value: TriggerType): String = value.name

    @TypeConverter
    fun toTriggerType(value: String): TriggerType = runCatching { TriggerType.valueOf(value) }.getOrDefault(TriggerType.MANUAL_SOS)

    @TypeConverter
    fun fromUploadStatus(value: UploadStatus): String = value.name

    @TypeConverter
    fun toUploadStatus(value: String): UploadStatus = runCatching { UploadStatus.valueOf(value) }.getOrDefault(UploadStatus.PENDING)
}
