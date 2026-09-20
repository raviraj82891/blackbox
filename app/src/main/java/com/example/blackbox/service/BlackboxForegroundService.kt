package com.example.blackbox.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.blackbox.MainActivity
import com.example.blackbox.R
import com.example.blackbox.data.db.EventType
import com.example.blackbox.data.db.TriggerType
import com.example.blackbox.data.repository.BlackboxRepository
import com.example.blackbox.data.repository.SensorBatchItem
import com.example.blackbox.domain.trigger.TriggerDetector
import com.example.blackbox.sensor.ActivityRecognitionCollector
import com.example.blackbox.sensor.AudioClassifierCollector
import com.example.blackbox.sensor.BatteryCollector
import com.example.blackbox.sensor.BatteryReading
import com.example.blackbox.sensor.LocationCollector
import com.example.blackbox.sensor.SensorCollector
import com.example.blackbox.sensor.UserActivityState
import com.example.blackbox.sensor.WifiSnapshotCollector
import com.example.blackbox.util.PermissionValidator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
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

    private var sensorCollectionJob: Job? = null
    private var locationJob: Job? = null
    private var currentIsMovingState = false

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    // Bounded channels for non-blocking persistence handoff (capacity = 50 batches = 2,500 events ~ 50s telemetry buffer)
    private val accelChannel = Channel<List<SensorBatchItem>>(capacity = 50, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val gyroChannel = Channel<List<SensorBatchItem>>(capacity = 50, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private var accelOverflowCount = 0L
    private var gyroOverflowCount = 0L

    // Debug-only heartbeat counters
    private var accelSampleCount = 0L
    private var gyroSampleCount = 0L
    private var detectorInputCount = 0L
    private var lastAccelTimestampMs = 0L
    private var lastGyroTimestampMs = 0L
    private var lastDetectorInputTimestampMs = 0L

    inner class LocalBinder : Binder() {
        fun getService(): BlackboxForegroundService = this@BlackboxForegroundService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        Log.d("TRACE_FALL_DEBUG", "protection_requested=true")
        ProtectionStateManager.updateState(ProtectionState.STARTING)
        Log.d("TRACE_FALL_DEBUG", "protection_state=${ProtectionStateManager.state.value}")

        try {
            val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
            val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            val gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

            val isAccelAvailable = accelSensor != null
            val isGyroAvailable = gyroSensor != null

            Log.d("TRACE_FALL_DEBUG", "service_started=true")
            Log.d("TRACE_FALL_DEBUG", "sensor_collector_started=true")
            Log.d("TRACE_FALL_DEBUG", "accelerometer_available=$isAccelAvailable")
            Log.d("TRACE_FALL_DEBUG", "gyroscope_available=$isGyroAvailable")

            if (!isAccelAvailable) {
                Log.e("TRACE_FALL_DEBUG", "FALL_DETECTION_UNSUPPORTED: Accelerometer hardware sensor not present")
                ProtectionStateManager.updateState(
                    ProtectionState.ERROR,
                    "FALL_DETECTION_UNSUPPORTED: Accelerometer hardware sensor not present on device"
                )
                return
            }

            startForegroundSafely()
            startSensorCollection()

            Log.d("TRACE_FALL_DEBUG", "accelerometer_registered=true")
            Log.d("TRACE_FALL_DEBUG", "gyroscope_registered=$isGyroAvailable")
            Log.d("TRACE_FALL_DEBUG", "detector_started=true")

            val missingOptional = PermissionValidator.getMissingOptionalPermissions(this)
            if (missingOptional.isNotEmpty()) {
                ProtectionStateManager.updateState(
                    ProtectionState.ACTIVE,
                    "Core motion protection active. Optional enhancements disabled: ${missingOptional.joinToString(", ")}"
                )
            } else {
                ProtectionStateManager.updateState(ProtectionState.ACTIVE)
            }
            Log.d("TRACE_FALL_DEBUG", "protection_state=${ProtectionStateManager.state.value}")
        } catch (e: Exception) {
            Log.e("TRACE_FALL_DEBUG", "Service startup error: ${e.localizedMessage}")
            ProtectionStateManager.updateState(
                ProtectionState.ERROR,
                e.localizedMessage ?: "Failed to start service foreground"
            )
        }
    }

    private fun startForegroundSafely() {
        val notification = buildNotification("TRACE Protection Active — Recording Your Last Hour")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var typeMask = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                typeMask = typeMask or ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            }
            if (PermissionValidator.hasLocationPermission(this)) {
                typeMask = typeMask or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && PermissionValidator.hasMicPermission(this)) {
                typeMask = typeMask or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            }
            if (typeMask == 0) {
                typeMask = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                }
            }
            try {
                ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, typeMask)
            } catch (_: Exception) {
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundSafely()
        when (intent?.action) {
            ACTION_STOP_SERVICE -> stopSelf()
            ACTION_PAUSE -> pauseCollection()
            ACTION_RESUME -> resumeCollection()
            ACTION_MANUAL_SOS -> triggerManualSos()
            else -> {
                if (!_isRecording.value) {
                    resumeCollection()
                }
            }
        }
        return START_STICKY
    }

    private fun startSensorCollection() {
        _isRecording.value = true
        sensorCollectionJob?.cancel()

        sensorCollectionJob = serviceScope.launch {
            val sensorCollector = SensorCollector(applicationContext)
            val locationCollector = LocationCollector(applicationContext)
            val activityCollector = ActivityRecognitionCollector(applicationContext)
            val audioCollector = AudioClassifierCollector(applicationContext)
            val wifiCollector = WifiSnapshotCollector(applicationContext)
            val batteryCollector = BatteryCollector(applicationContext)

            val accelBatch = ArrayList<SensorBatchItem>(50)
            val gyroBatch = ArrayList<SensorBatchItem>(50)

            // Dedicated Accelerometer Persistence Consumer Coroutine
            launch(Dispatchers.IO) {
                for (batch in accelChannel) {
                    if (!_isRecording.value) continue
                    try {
                        repository.recordSensorEventsBatch(batch)
                    } catch (e: Exception) {
                        Log.e("TRACE_FALL_DEBUG", "Accel batch DB write error: ${e.localizedMessage}")
                    }
                }
            }

            // Dedicated Gyroscope Persistence Consumer Coroutine
            launch(Dispatchers.IO) {
                for (batch in gyroChannel) {
                    if (!_isRecording.value) continue
                    try {
                        repository.recordSensorEventsBatch(batch)
                    } catch (e: Exception) {
                        Log.e("TRACE_FALL_DEBUG", "Gyro batch DB write error: ${e.localizedMessage}")
                    }
                }
            }

            // 1. Accelerometer Flow (50Hz Real-Time Physics + Non-Blocking Persistence Handoff)
            launch {
                sensorCollector.observeAccelerometer().collect { reading ->
                    if (!_isRecording.value) return@collect

                    accelSampleCount++
                    detectorInputCount++
                    lastAccelTimestampMs = reading.timestampMs
                    lastDetectorInputTimestampMs = reading.timestampMs

                    // Real-Time 50Hz Physics Trigger Evaluation in Memory (Zero Latency)
                    triggerDetector.evaluateMotion(reading.magnitude, reading.timestampMs)

                    val payload = """{"x":%.2f,"y":%.2f,"z":%.2f,"magnitude":%.2f}""".format(
                        reading.x, reading.y, reading.z, reading.magnitude
                    )

                    val isSpike = reading.magnitude > 18.0f
                    synchronized(accelBatch) {
                        accelBatch.add(SensorBatchItem(EventType.ACCEL, payload, reading.timestampMs))
                    }

                    // Flush accelBatch every 50 samples (~1s) or immediately on high impact spike
                    if (isSpike || accelBatch.size >= 50) {
                        val toFlush = synchronized(accelBatch) {
                            val copy = ArrayList(accelBatch)
                            accelBatch.clear()
                            copy
                        }
                        if (toFlush.isNotEmpty()) {
                            val res = accelChannel.trySend(toFlush)
                            if (res.isFailure) {
                                accelOverflowCount++
                                Log.w("TRACE_FALL_DEBUG", "accel_persistence_queue_overflow count=$accelOverflowCount")
                            }
                        }
                    }
                }
            }

            // 2. Gyroscope Flow (50Hz Real-Time Physics + Non-Blocking Persistence Handoff)
            launch {
                sensorCollector.observeGyroscope().collect { reading ->
                    if (!_isRecording.value) return@collect

                    gyroSampleCount++
                    lastGyroTimestampMs = reading.timestampMs

                    val payload = """{"x":%.2f,"y":%.2f,"z":%.2f,"deltaMagnitude":%.2f}""".format(
                        reading.x, reading.y, reading.z, reading.deltaMagnitude
                    )

                    // Update latest gyroscopic rotation magnitude and timestamp
                    triggerDetector.updateGyroscope(reading.deltaMagnitude, reading.timestampMs)

                    val isGyroSpike = reading.deltaMagnitude > 2.0f
                    synchronized(gyroBatch) {
                        gyroBatch.add(SensorBatchItem(EventType.GYRO, payload, reading.timestampMs))
                    }

                    if (isGyroSpike || gyroBatch.size >= 50) {
                        val toFlush = synchronized(gyroBatch) {
                            val copy = ArrayList(gyroBatch)
                            gyroBatch.clear()
                            copy
                        }
                        if (toFlush.isNotEmpty()) {
                            val res = gyroChannel.trySend(toFlush)
                            if (res.isFailure) {
                                gyroOverflowCount++
                                Log.w("TRACE_FALL_DEBUG", "gyro_persistence_queue_overflow count=$gyroOverflowCount")
                            }
                        }
                    }
                }
            }

            // 3. Activity Recognition Transition Flow
            if (PermissionValidator.hasActivityPermission(applicationContext)) {
                launch {
                    activityCollector.startTransitionUpdates().collect { activityReading ->
                        if (!_isRecording.value) return@collect
                        val payload = """{"state":"${activityReading.state.name}","confidence":${activityReading.confidence}}"""
                        repository.recordSensorEvent(EventType.ACTIVITY, payload, activityReading.timestampMs)

                        val isMoving = activityReading.state == UserActivityState.WALKING ||
                                activityReading.state == UserActivityState.RUNNING ||
                                activityReading.state == UserActivityState.IN_VEHICLE

                        val bat = batteryCollector.getBatteryStatus()
                        val isPowerSaver = bat.levelPercentage < 15 && !bat.isCharging

                        val effectiveIsMoving = if (isPowerSaver) false else isMoving

                        if (effectiveIsMoving != currentIsMovingState || locationJob == null) {
                            currentIsMovingState = effectiveIsMoving
                            restartLocationSampling(locationCollector, effectiveIsMoving)
                        }
                    }
                }
            }

            // Initial location sampling
            if (PermissionValidator.hasLocationPermission(applicationContext)) {
                restartLocationSampling(locationCollector, isMoving = false)
            }

            // 4. Audio Classifier Flow
            if (PermissionValidator.hasMicPermission(applicationContext)) {
                launch {
                    audioCollector.observeAudioEvents().collect { audioEvent ->
                        if (!_isRecording.value) return@collect
                        val bat = batteryCollector.getBatteryStatus()
                        if (bat.levelPercentage < 15 && !bat.isCharging) return@collect

                        val payload = """{"eventLabel":"${audioEvent.eventLabel}","decibels":%.1f,"confidence":%.2f}""".format(
                            audioEvent.decibels, audioEvent.confidence
                        )
                        repository.recordSensorEvent(EventType.AUDIO_EVENT, payload, audioEvent.timestampMs)
                    }
                }
            }

            // 5. Battery & Environment periodic sampling
            launch {
                val bat = batteryCollector.getBatteryStatus()
                val batPayload = """{"level":${bat.levelPercentage},"isCharging":${bat.isCharging}}"""
                repoPayload(batPayload, bat)

                val wifi = wifiCollector.captureSnapshot()
                val wifiPayload = """{"ssid":"${wifi.connectedSsid ?: ""}","signalDbm":${wifi.signalLevelDbm},"accessPoints":${wifi.nearbyAccessPointCount}}"""
                repository.recordSensorEvent(EventType.WIFI, wifiPayload, wifi.timestampMs)
            }

            // 6. Periodic Rolling Buffer Purge Maintenance
            launch {
                while (isActive) {
                    delay(15 * 60 * 1000L)
                    if (_isRecording.value) {
                        repository.purgeExpiredBuffer(60)
                    }
                }
            }

            // 7. TRACE_FALL_DEBUG 2-Second Heartbeat Logging Loop
            launch {
                while (isActive) {
                    delay(2000L)
                    if (_isRecording.value) {
                        val now = System.currentTimeMillis()
                        val accelAge = if (lastAccelTimestampMs > 0) now - lastAccelTimestampMs else -1
                        val gyroAge = if (lastGyroTimestampMs > 0) now - lastGyroTimestampMs else -1
                        val detectorAge = if (lastDetectorInputTimestampMs > 0) now - lastDetectorInputTimestampMs else -1
                        Log.d(
                            "TRACE_FALL_DEBUG",
                            "state=${ProtectionStateManager.state.value} accel_samples=$accelSampleCount gyro_samples=$gyroSampleCount detector_inputs=$detectorInputCount last_accel_age_ms=$accelAge last_gyro_age_ms=$gyroAge detector_last_input_age_ms=$detectorAge accel_overflows=$accelOverflowCount gyro_overflows=$gyroOverflowCount detector_state=${triggerDetector.countdownState.value}"
                        )
                    }
                }
            }
        }
    }

    private suspend fun repoPayload(batPayload: String, bat: BatteryReading) {
        repository.recordSensorEvent(EventType.BATTERY, batPayload, bat.timestampMs)
    }

    private fun restartLocationSampling(locationCollector: LocationCollector, isMoving: Boolean) {
        if (!PermissionValidator.hasLocationPermission(applicationContext)) return
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
        sensorCollectionJob?.cancel()
        sensorCollectionJob = null
        locationJob?.cancel()
        locationJob = null
        triggerDetector.resetState()
        ProtectionStateManager.updateState(ProtectionState.PAUSED)
        Log.d("TRACE_FALL_DEBUG", "protection_state=${ProtectionStateManager.state.value}")
        updateNotification("TRACE Protection Paused — Sensor Buffer Off")
    }

    fun resumeCollection() {
        startSensorCollection()

        val missingOptional = PermissionValidator.getMissingOptionalPermissions(this)
        if (missingOptional.isNotEmpty()) {
            ProtectionStateManager.updateState(
                ProtectionState.ACTIVE,
                "Core motion protection active. Optional enhancements disabled: ${missingOptional.joinToString(", ")}"
            )
        } else {
            ProtectionStateManager.updateState(ProtectionState.ACTIVE)
        }
        Log.d("TRACE_FALL_DEBUG", "protection_state=${ProtectionStateManager.state.value}")
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
        _isRecording.value = false
        sensorCollectionJob?.cancel()
        sensorCollectionJob = null
        locationJob?.cancel()
        locationJob = null
        triggerDetector.resetState()
        ProtectionStateManager.updateState(ProtectionState.STOPPED)
        Log.d("TRACE_FALL_DEBUG", "protection_state=${ProtectionStateManager.state.value}")
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
