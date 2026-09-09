package com.example.blackbox.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.blackbox.MainActivity
import com.example.blackbox.R
import com.example.blackbox.data.db.EventType
import com.example.blackbox.data.db.TriggerType
import com.example.blackbox.data.repository.BlackboxRepository
import com.example.blackbox.domain.trigger.TriggerDetector
import com.example.blackbox.sensor.ActivityRecognitionCollector
import com.example.blackbox.sensor.AudioClassifierCollector
import com.example.blackbox.sensor.BatteryCollector
import com.example.blackbox.sensor.BatteryReading
import com.example.blackbox.sensor.LocationCollector
import com.example.blackbox.sensor.SensorCollector
import com.example.blackbox.sensor.UserActivityState
import com.example.blackbox.sensor.WifiSnapshotCollector
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BlackboxForegroundService : Service() {

    @Inject
    lateinit var repository: BlackboxRepository

    @Inject
    lateinit var triggerDetector: TriggerDetector

    private val binder = LocalBinder()
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private var locationJob: Job? = null
    private var currentIsMovingState = false

    inner class LocalBinder : Binder() {
        fun getService(): BlackboxForegroundService = this@BlackboxForegroundService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        startForeground(NOTIFICATION_ID, buildNotification("TRACE Protection Active — Recording Your Last Hour"))
        startSensorCollection()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_SERVICE -> stopSelf()
            ACTION_PAUSE -> pauseCollection()
            ACTION_RESUME -> resumeCollection()
            ACTION_MANUAL_SOS -> triggerManualSos()
        }
        return START_STICKY
    }

    private fun startSensorCollection() {
        _isRecording.value = true

        val sensorCollector = SensorCollector(applicationContext)
        val locationCollector = LocationCollector(applicationContext)
        val activityCollector = ActivityRecognitionCollector(applicationContext)
        val audioCollector = AudioClassifierCollector(applicationContext)
        val wifiCollector = WifiSnapshotCollector(applicationContext)
        val batteryCollector = BatteryCollector(applicationContext)

        // 1. Accelerometer Flow
        serviceScope.launch {
            sensorCollector.observeAccelerometer().collect { reading ->
                if (!_isRecording.value) return@collect
                val payload = """{"x":%.2f,"y":%.2f,"z":%.2f,"magnitude":%.2f}""".format(
                    reading.x, reading.y, reading.z, reading.magnitude
                )
                repository.recordSensorEvent(EventType.ACCEL, payload, reading.timestampMs)

                // Feed into physics trigger detector
                triggerDetector.evaluateMotion(reading.magnitude, reading.timestampMs)
            }
        }

        // 2. Gyroscope Flow
        serviceScope.launch {
            sensorCollector.observeGyroscope().collect { reading ->
                if (!_isRecording.value) return@collect
                val payload = """{"x":%.2f,"y":%.2f,"z":%.2f,"deltaMagnitude":%.2f}""".format(
                    reading.x, reading.y, reading.z, reading.deltaMagnitude
                )
                repository.recordSensorEvent(EventType.GYRO, payload, reading.timestampMs)

                // Update latest gyroscopic rotation magnitude
                triggerDetector.updateGyroscope(reading.deltaMagnitude)
            }
        }

        // 3. Activity Recognition Transition Flow -> Dynamically drives location sampling
        serviceScope.launch {
            activityCollector.startTransitionUpdates().collect { activityReading ->
                if (!_isRecording.value) return@collect
                val payload = """{"state":"${activityReading.state.name}","confidence":${activityReading.confidence}}"""
                repository.recordSensorEvent(EventType.ACTIVITY, payload, activityReading.timestampMs)

                // Derive movement status dynamically: STILL/UNKNOWN -> false, WALKING/RUNNING/IN_VEHICLE -> true
                val isMoving = activityReading.state == UserActivityState.WALKING ||
                        activityReading.state == UserActivityState.RUNNING ||
                        activityReading.state == UserActivityState.IN_VEHICLE

                val bat = batteryCollector.getBatteryStatus()
                val isPowerSaver = bat.levelPercentage < 15 && !bat.isCharging

                // Battery Saver forces low power interval regardless of movement
                val effectiveIsMoving = if (isPowerSaver) false else isMoving

                if (effectiveIsMoving != currentIsMovingState || locationJob == null) {
                    currentIsMovingState = effectiveIsMoving
                    restartLocationSampling(locationCollector, effectiveIsMoving)
                }
            }
        }

        // Initial location sampling
        restartLocationSampling(locationCollector, isMoving = false)

        // 4. Audio Classifier Flow (Battery Saver pauses audio classifier)
        serviceScope.launch {
            audioCollector.observeAudioEvents().collect { audioEvent ->
                if (!_isRecording.value) return@collect
                val bat = batteryCollector.getBatteryStatus()
                if (bat.levelPercentage < 15 && !bat.isCharging) return@collect // Pause in Power Saver Mode

                val payload = """{"eventLabel":"${audioEvent.eventLabel}","decibels":%.1f,"confidence":%.2f}""".format(
                    audioEvent.decibels, audioEvent.confidence
                )
                repository.recordSensorEvent(EventType.AUDIO_EVENT, payload, audioEvent.timestampMs)
            }
        }

        // 5. Battery & Environment periodic sampling
        serviceScope.launch {
            val bat = batteryCollector.getBatteryStatus()
            val batPayload = """{"level":${bat.levelPercentage},"isCharging":${bat.isCharging}}"""
            repoPayload(batPayload, bat)

            val wifi = wifiCollector.captureSnapshot()
            val wifiPayload = """{"ssid":"${wifi.connectedSsid ?: ""}","signalDbm":${wifi.signalLevelDbm},"accessPoints":${wifi.nearbyAccessPointCount}}"""
            repository.recordSensorEvent(EventType.WIFI, wifiPayload, wifi.timestampMs)
        }
    }

    private suspend fun repoPayload(batPayload: String, bat: BatteryReading) {
        repository.recordSensorEvent(EventType.BATTERY, batPayload, bat.timestampMs)
    }

    private fun restartLocationSampling(locationCollector: LocationCollector, isMoving: Boolean) {
        locationJob?.cancel()
        locationJob = serviceScope.launch {
            locationCollector.observeAdaptiveLocation(isMoving = isMoving).collect { loc ->
                if (!_isRecording.value) return@collect
                val payload = """{"latitude":%.6f,"longitude":%.6f,"speed":%.2f,"accuracy":%.1f}""".format(
                    loc.latitude, loc.longitude, loc.speed, loc.accuracy
                )
                repository.recordSensorEvent(EventType.LOCATION, payload, loc.timestampMs)
            }
        }
    }

    fun pauseCollection() {
        _isRecording.value = false
        updateNotification("TRACE Protection Paused — Sensor Buffer Off")
    }

    fun resumeCollection() {
        _isRecording.value = true
        updateNotification("TRACE Protection Active — Recording Your Last Hour")
    }

    fun triggerManualSos() {
        triggerDetector.startCountdown(TriggerType.MANUAL_SOS, durationSeconds = 3)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "TRACE Protection Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active status of your TRACE privacy-preserving safety buffer"
            }
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TRACE — Digital Black Box")
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(contentText))
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    companion object {
        const val CHANNEL_ID = "trace_service_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_MANUAL_SOS = "ACTION_MANUAL_SOS"
    }
}
