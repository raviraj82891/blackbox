package com.example.blackbox.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ProtectionState {
    STARTING,
    ACTIVE,
    PAUSED,
    PERMISSION_LIMITED,
    ERROR,
    STOPPED
}

/**
 * Single Authoritative Source of Truth for TRACE Protection Service State.
 * Prevents UI desync on service crashes, killed processes, or missing permissions.
 */
object ProtectionStateManager {

    private val _state = MutableStateFlow(ProtectionState.STOPPED)
    val state: StateFlow<ProtectionState> = _state.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    fun updateState(newState: ProtectionState, errorMsg: String? = null) {
        _state.value = newState
        _lastError.value = errorMsg
    }

    fun resetState() {
        _state.value = ProtectionState.STOPPED
        _lastError.value = null
    }
}
