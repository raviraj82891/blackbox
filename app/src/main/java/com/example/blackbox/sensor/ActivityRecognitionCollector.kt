package com.example.blackbox.sensor

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class UserActivityState {
    STILL,
    WALKING,
    RUNNING,
    IN_VEHICLE,
    UNKNOWN
}

data class ActivityReading(
    val state: UserActivityState,
    val confidence: Int,
    val timestampMs: Long
)

class ActivityRecognitionCollector(private val context: Context) {

    private val _currentActivity = MutableStateFlow(
        ActivityReading(UserActivityState.STILL, 100, System.currentTimeMillis())
    )

    fun observeActivity(): Flow<ActivityReading> = _currentActivity.asStateFlow()

    fun updateActivityState(state: UserActivityState, confidence: Int) {
        _currentActivity.value = ActivityReading(state, confidence, System.currentTimeMillis())
    }
}
