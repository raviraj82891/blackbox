package com.example.blackbox.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.blackbox.data.crypto.KeyManagementService
import com.example.blackbox.data.db.TriggerType
import com.example.blackbox.data.db.UploadStatus
import com.example.blackbox.data.repository.BlackboxRepository
import com.example.blackbox.domain.trigger.TriggerDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class AnalyticsState(
    val totalSensorEvents: Int = 0,
    val autoCrashCount: Int = 0,
    val autoFallCount: Int = 0,
    val manualSosCount: Int = 0,
    val cancelledCountdownsCount: Int = 0,
    val safetyCheckInCount: Int = 0,
    val successfulUploadsCount: Int = 0,
    val failedUploadsCount: Int = 0,
    val hasData: Boolean = false,
    val timeRangeLabel: String = "Since installation"
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    application: Application,
    val repository: BlackboxRepository,
    val triggerDetector: TriggerDetector
) : AndroidViewModel(application) {

    private val kms = KeyManagementService(application)

    val analyticsState: StateFlow<AnalyticsState> = combine(
        repository.getBufferEventCount(),
        repository.getAllIncidentReports()
    ) { totalEvents, reports ->
        val cancelledCount = kms.getCancelledCountdownsCount()
        val checkInCount = kms.getSafetyCheckInCount()

        val autoCrash = reports.count { it.triggerType == TriggerType.AUTO_CRASH }
        val autoFall = reports.count { it.triggerType == TriggerType.AUTO_FALL }
        val manualSos = reports.count { it.triggerType == TriggerType.MANUAL_SOS }
        val successUploads = reports.count { it.uploadStatus == UploadStatus.UPLOADED }
        val failedUploads = reports.count { it.uploadStatus == UploadStatus.FAILED }

        val hasAnyData = totalEvents > 0 || reports.isNotEmpty() || cancelledCount > 0 || checkInCount > 0

        AnalyticsState(
            totalSensorEvents = totalEvents,
            autoCrashCount = autoCrash,
            autoFallCount = autoFall,
            manualSosCount = manualSos,
            cancelledCountdownsCount = cancelledCount,
            safetyCheckInCount = checkInCount,
            successfulUploadsCount = successUploads,
            failedUploadsCount = failedUploads,
            hasData = hasAnyData,
            timeRangeLabel = "Since installation"
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnalyticsState()
    )
}
