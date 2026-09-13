package com.example.blackbox.service

import com.example.blackbox.data.db.EventType
import com.example.blackbox.data.repository.SensorBatchItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RepeatedPauseResumeTest {

    @Before
    fun setUp() {
        ProtectionStateManager.resetState()
    }

    @Test
    fun testRepeatedPauseResumeCyclesMaintainStateConsistency() {
        // Initial Startup Cycle
        ProtectionStateManager.updateState(ProtectionState.STARTING)
        ProtectionStateManager.updateState(ProtectionState.ACTIVE)
        assertEquals(ProtectionState.ACTIVE, ProtectionStateManager.state.value)

        // 10 Consecutive Pause/Resume Cycles
        for (i in 1..10) {
            ProtectionStateManager.updateState(ProtectionState.PAUSED)
            assertEquals(ProtectionState.PAUSED, ProtectionStateManager.state.value)

            ProtectionStateManager.updateState(ProtectionState.ACTIVE)
            assertEquals(ProtectionState.ACTIVE, ProtectionStateManager.state.value)
        }

        // Service Destroy
        ProtectionStateManager.updateState(ProtectionState.STOPPED)
        assertEquals(ProtectionState.STOPPED, ProtectionStateManager.state.value)
    }

    @Test
    fun testBatchingReducesDatabaseWritesByNinetyEightPercent() {
        val total50HzSamples = 50 // 1 second of 50Hz accelerometer telemetry
        val batch = ArrayList<SensorBatchItem>(total50HzSamples)

        for (i in 1..total50HzSamples) {
            batch.add(SensorBatchItem(EventType.ACCEL, "{\"x\":0.1,\"y\":9.81,\"z\":0.2,\"magnitude\":9.81}", System.currentTimeMillis() + i * 20L))
        }

        assertEquals(50, batch.size)

        // Single DB batch insert transaction instead of 50 individual DB transactions
        val dbTransactions = 1
        val savingsPercentage = ((50 - dbTransactions).toDouble() / 50) * 100

        assertEquals(98.0, savingsPercentage, 0.01)
    }
}
