package com.example.blackbox.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blackbox.data.db.SensorEvent
import com.example.blackbox.data.repository.BlackboxRepository
import com.example.blackbox.domain.fusion.FusionEngine
import com.example.blackbox.domain.fusion.TimelineSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val repository: BlackboxRepository
) : ViewModel() {

    val rawEvents: StateFlow<List<SensorEvent>> = repository.getRollingBufferEvents(60)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessions: StateFlow<List<TimelineSession>> = rawEvents.map { events ->
        val flatEntries = FusionEngine.reconstructTimeline(events)
        FusionEngine.groupTimelineIntoSessions(flatEntries)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isChainValid = MutableStateFlow(true)
    val isChainValid: StateFlow<Boolean> = _isChainValid.asStateFlow()

    init {
        viewModelScope.launch {
            _isChainValid.value = repository.verifyBufferIntegrity(60)
        }
    }
}
