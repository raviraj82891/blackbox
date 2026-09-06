package com.example.blackbox.sensor

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow

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

/**
 * Live Activity Recognition Collector wired to Google Play Services ActivityRecognitionClient.
 * Tracks transitions between STILL, WALKING, RUNNING, and IN_VEHICLE.
 */
class ActivityRecognitionCollector(private val context: Context) {

    private val activityRecognitionClient = ActivityRecognition.getClient(context)
    private val _currentActivity = MutableStateFlow(
        ActivityReading(UserActivityState.STILL, 100, System.currentTimeMillis())
    )

    fun observeActivity(): Flow<ActivityReading> = _currentActivity.asStateFlow()

    @SuppressLint("MissingPermission")
    fun startTransitionUpdates(): Flow<ActivityReading> = callbackFlow {
        val intent = Intent(ACTION_ACTIVITY_TRANSITION)
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, pendingIntentFlags)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent != null && ActivityTransitionResult.hasResult(intent)) {
                    val result = ActivityTransitionResult.extractResult(intent) ?: return
                    for (event in result.transitionEvents) {
                        if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                            val mappedState = when (event.activityType) {
                                DetectedActivity.IN_VEHICLE -> UserActivityState.IN_VEHICLE
                                DetectedActivity.WALKING -> UserActivityState.WALKING
                                DetectedActivity.RUNNING -> UserActivityState.RUNNING
                                DetectedActivity.STILL -> UserActivityState.STILL
                                else -> UserActivityState.UNKNOWN
                            }
                            val reading = ActivityReading(mappedState, 100, System.currentTimeMillis())
                            _currentActivity.value = reading
                            trySend(reading)
                        }
                    }
                }
            }
        }

        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(ACTION_ACTIVITY_TRANSITION),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        val transitions = mutableListOf<ActivityTransition>()

        val activityTypes = listOf(
            DetectedActivity.IN_VEHICLE,
            DetectedActivity.WALKING,
            DetectedActivity.RUNNING,
            DetectedActivity.STILL
        )

        for (type in activityTypes) {
            transitions.add(
                ActivityTransition.Builder()
                    .setActivityType(type)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build()
            )
            transitions.add(
                ActivityTransition.Builder()
                    .setActivityType(type)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build()
            )
        }

        val request = ActivityTransitionRequest(transitions)

        try {
            activityRecognitionClient.requestActivityTransitionUpdates(request, pendingIntent)
        } catch (e: Exception) {
            // Permission or device fallback
        }

        awaitClose {
            try {
                activityRecognitionClient.removeActivityTransitionUpdates(pendingIntent)
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun updateActivityState(state: UserActivityState, confidence: Int) {
        _currentActivity.value = ActivityReading(state, confidence, System.currentTimeMillis())
    }

    companion object {
        const val ACTION_ACTIVITY_TRANSITION = "com.example.blackbox.ACTION_ACTIVITY_TRANSITION"
    }
}
