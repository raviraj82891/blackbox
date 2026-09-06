package com.example.blackbox.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blackbox.data.db.TriggerType
import com.example.blackbox.data.repository.BlackboxRepository
import com.example.blackbox.domain.trigger.TriggerDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    repository: BlackboxRepository,
    val triggerDetector: TriggerDetector
) : ViewModel() {

    val totalEventsCount: StateFlow<Int> = repository.getBufferEventCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val reports = repository.getAllIncidentReports()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val autoCrashCount: StateFlow<Int> = reports.map { list ->
        list.count { it.triggerType == TriggerType.AUTO_CRASH || it.triggerType == TriggerType.AUTO_FALL }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val manualSosCount: StateFlow<Int> = reports.map { list ->
        list.count { it.triggerType == TriggerType.MANUAL_SOS }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
}
