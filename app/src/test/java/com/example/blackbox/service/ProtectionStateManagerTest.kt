package com.example.blackbox.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ProtectionStateManagerTest {

    @Before
    fun setUp() {
        ProtectionStateManager.resetState()
    }

    @Test
    fun testInitialStateIsStopped() {
        assertEquals(ProtectionState.STOPPED, ProtectionStateManager.state.value)
        assertNull(ProtectionStateManager.lastError.value)
    }

    @Test
    fun testCompleteSuccessfulServiceLifecycle() {
        // 1. Initial state
        assertEquals(ProtectionState.STOPPED, ProtectionStateManager.state.value)

        // 2. Service starting
        ProtectionStateManager.updateState(ProtectionState.STARTING)
        assertEquals(ProtectionState.STARTING, ProtectionStateManager.state.value)

        // 3. Service initialization succeeded & active
        ProtectionStateManager.updateState(ProtectionState.ACTIVE)
        assertEquals(ProtectionState.ACTIVE, ProtectionStateManager.state.value)

        // 4. User pauses protection
        ProtectionStateManager.updateState(ProtectionState.PAUSED)
        assertEquals(ProtectionState.PAUSED, ProtectionStateManager.state.value)

        // 5. User resumes protection
        ProtectionStateManager.updateState(ProtectionState.ACTIVE)
        assertEquals(ProtectionState.ACTIVE, ProtectionStateManager.state.value)

        // 6. Service destroyed / stopped
        ProtectionStateManager.updateState(ProtectionState.STOPPED)
        assertEquals(ProtectionState.STOPPED, ProtectionStateManager.state.value)
    }

    @Test
    fun testServiceStartFailureTransitionsToErrorState() {
        ProtectionStateManager.updateState(ProtectionState.STARTING)
        ProtectionStateManager.updateState(ProtectionState.ERROR, "ForegroundServiceStartNotAllowedException: Service start disallowed in background")

        assertEquals(ProtectionState.ERROR, ProtectionStateManager.state.value)
        assertEquals("ForegroundServiceStartNotAllowedException: Service start disallowed in background", ProtectionStateManager.lastError.value)
    }

    @Test
    fun testResumeWithMissingPermissionsTransitionsToPermissionLimited() {
        ProtectionStateManager.updateState(ProtectionState.ACTIVE)
        assertEquals(ProtectionState.ACTIVE, ProtectionStateManager.state.value)

        ProtectionStateManager.updateState(ProtectionState.PAUSED)
        assertEquals(ProtectionState.PAUSED, ProtectionStateManager.state.value)

        // User revoked permissions while paused -> Resume transitions to PERMISSION_LIMITED
        ProtectionStateManager.updateState(ProtectionState.PERMISSION_LIMITED, "Missing required permissions: Location")
        assertEquals(ProtectionState.PERMISSION_LIMITED, ProtectionStateManager.state.value)
        assertEquals("Missing required permissions: Location", ProtectionStateManager.lastError.value)
    }

    @Test
    fun testMissingPermissionsOnStartupTransitionsToPermissionLimited() {
        ProtectionStateManager.updateState(ProtectionState.STARTING)
        ProtectionStateManager.updateState(ProtectionState.PERMISSION_LIMITED, "Missing required permissions: Location, Microphone")

        assertEquals(ProtectionState.PERMISSION_LIMITED, ProtectionStateManager.state.value)
        assertEquals("Missing required permissions: Location, Microphone", ProtectionStateManager.lastError.value)
    }

    @Test
    fun testProcessDeathOrDestructionTransitionsToStopped() {
        ProtectionStateManager.updateState(ProtectionState.ACTIVE)
        assertEquals(ProtectionState.ACTIVE, ProtectionStateManager.state.value)

        // Process death / onDestroy
        ProtectionStateManager.updateState(ProtectionState.STOPPED)
        assertEquals(ProtectionState.STOPPED, ProtectionStateManager.state.value)
    }
}
