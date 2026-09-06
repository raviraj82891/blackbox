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
import com.example.blackbox.data.api.RetrofitClient
import com.example.blackbox.data.crypto.KeyManagementService
import com.example.blackbox.data.db.BlackboxDatabase
import com.example.blackbox.data.db.EventType
import com.example.blackbox.data.db.TriggerType
import com.example.blackbox.data.repository.BlackboxRepository
import com.example.blackbox.domain.trigger.TriggerDetector
import com.example.blackbox.sensor.AudioClassifierCollector
import com.example.blackbox.sensor.BatteryCollector
import com.example.blackbox.sensor.LocationCollector
import com.example.blackbox.sensor.SensorCollector
import com.example.blackbox.sensor.WifiSnapshotCollector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BlackboxForegroundService : Service() {

    private val binder = LocalBinder()
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private var repository: BlackboxRepository? = null
    val triggerDetector = TriggerDetector()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    inner class LocalBinder : Binder() {
        fun getService(): BlackboxForegroundService = this@BlackboxForegroundService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val kms = KeyManagementService(applicationContext)
        val dbPassphrase = kms.getOrCreateDatabasePassphrase()
        val db = BlackboxDatabase.getInstance(applicationContext, dbPassphrase)
        repository = BlackboxRepository(db.sensorEventDao(), db.incidentReportDao(), kms, RetrofitClient.apiService)

        startForeground(NOTIFICATION_ID, buildNotification("Black Box Active — Continuous 60m Buffer"))
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

        val repo = repository ?: return
        val sensorCollector = SensorCollector(applicationContext)
        val locationCollector = LocationCollector(applicationContext)
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
                repo.recordSensorEvent(EventType.ACCEL, payload, reading.timestampMs)

                // Feed into trigger detector
                triggerDetector.evaluateMotion(reading.magnitude, 0f, reading.timestampMs)
            }
        }

        // 2. Gyroscope Flow
        serviceScope.launch {
            sensorCollector.observeGyroscope().collect { reading ->
                if (!_isRecording.value) return@collect
                val payload = """{"x":%.2f,"y":%.2f,"z":%.2f,"deltaMagnitude":%.2f}""".format(
                    reading.x, reading.y, reading.z, reading.deltaMagnitude
                )
                repo.recordSensorEvent(EventType.GYRO, payload, reading.timestampMs)
            }
        }

        // 3. Audio Classifier Flow (NO RAW AUDIO PERSISTENCE)
        serviceScope.launch {
            audioCollector.observeAudioEvents().collect { audioEvent ->
                if (!_isRecording.value) return@collect
                val payload = """{"eventLabel":"${audioEvent.eventLabel}","decibels":%.1f,"confidence":%.2f}""".format(
                    audioEvent.decibels, audioEvent.confidence
                )
                repo.recordSensorEvent(EventType.AUDIO_EVENT, payload, audioEvent.timestampMs)
            }
        }

        // 4. Battery & Environment periodic sampling
        serviceScope.launch {
            val bat = batteryCollector.getBatteryStatus()
            val batPayload = """{"level":${bat.levelPercentage},"isCharging":${bat.isCharging}}"""
            repo.recordSensorEvent(EventType.BATTERY, batPayload, bat.timestampMs)

            val wifi = wifiCollector.captureSnapshot()
            val wifiPayload = """{"ssid":"${wifi.connectedSsid ?: ""}","signalDbm":${wifi.signalLevelDbm},"accessPoints":${wifi.nearbyAccessPointCount}}"""
            repo.recordSensorEvent(EventType.WIFI, wifiPayload, wifi.timestampMs)
        }
    }

    fun pauseCollection() {
        _isRecording.value = false
        updateNotification("Black Box Paused — Sensor Ingestion Off")
    }

    fun resumeCollection() {
        _isRecording.value = true
        updateNotification("Black Box Active — Continuous 60m Buffer")
    }

    fun triggerManualSos() {
        triggerDetector.startCountdown(TriggerType.MANUAL_SOS, 0.0)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Blackbox Protection Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active status of the privacy-preserving black box sensor buffer"
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
            .setContentTitle("Human Digital Black Box")
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
        const val CHANNEL_ID = "blackbox_service_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_MANUAL_SOS = "ACTION_MANUAL_SOS"
    }
}
