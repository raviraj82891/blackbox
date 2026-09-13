package com.example.blackbox.data.crypto

import com.example.blackbox.data.db.EventType
import com.example.blackbox.data.db.SensorEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HashChainManagerTest {

    private fun createHelperEvent(
        id: Long,
        timestampMs: Long,
        type: EventType,
        payloadJson: String,
        prevHash: String
    ): SensorEvent {
        val entryHash = HashChainManager.computeEntryHash(prevHash, payloadJson, timestampMs)
        return SensorEvent(
            id = id,
            timestampMs = timestampMs,
            type = type,
            payloadJson = payloadJson,
            prevHash = prevHash,
            entryHash = entryHash
        )
    }

    private fun generateSampleChain(count: Int, startPrevHash: String = HashChainManager.INITIAL_HASH): List<SensorEvent> {
        val list = mutableListOf<SensorEvent>()
        var currentPrevHash = startPrevHash
        val baseTime = 1700000000000L

        for (i in 1..count) {
            val timestamp = baseTime + (i * 1000L)
            val payload = """{"eventIndex":$i,"magnitude":${9.8 + (i * 0.1)}}"""
            val event = createHelperEvent(
                id = i.toLong(),
                timestampMs = timestamp,
                type = EventType.ACCEL,
                payloadJson = payload,
                prevHash = currentPrevHash
            )
            list.add(event)
            currentPrevHash = event.entryHash
        }
        return list
    }

    @Test
    fun testIntactCompleteChainWithGenesisAnchor() {
        val chain = generateSampleChain(10, HashChainManager.INITIAL_HASH)
        assertTrue(
            "Intact complete chain starting from INITIAL_HASH must pass boundary verification",
            HashChainManager.verifyChainIntegrity(chain, expectedBoundaryHash = HashChainManager.INITIAL_HASH)
        )
    }

    @Test
    fun testBoundaryAnchorVerificationAfterPruning() {
        val fullChain = generateSampleChain(15, HashChainManager.INITIAL_HASH)
        val lastPurgedEvent = fullChain[4] // 5th event (index 4)
        val boundaryAnchorHash = lastPurgedEvent.entryHash

        val retainedChain = fullChain.subList(5, 15)

        // Retained chain first event (index 5) has prevHash == lastPurgedEvent.entryHash (boundaryAnchorHash)
        assertTrue(
            "Retained chain verified against persisted boundary anchor hash must pass",
            HashChainManager.verifyChainIntegrity(retainedChain, expectedBoundaryHash = boundaryAnchorHash)
        )
    }

    @Test
    fun testModifiedFirstRetainedEvent_PayloadTampered() {
        val fullChain = generateSampleChain(10, HashChainManager.INITIAL_HASH)
        val boundaryAnchorHash = fullChain[2].entryHash
        val retainedChain = fullChain.subList(3, 10).toMutableList()

        // Attacker modifies the payload of the first retained event
        val tamperedFirst = retainedChain[0].copy(payloadJson = """{"tampered":true}""")
        retainedChain[0] = tamperedFirst

        assertFalse(
            "Tampering payload of the first retained event must cause verification failure",
            HashChainManager.verifyChainIntegrity(retainedChain, expectedBoundaryHash = boundaryAnchorHash)
        )
    }

    @Test
    fun testModifiedFirstRetainedEvent_PrevHashTampered() {
        val fullChain = generateSampleChain(10, HashChainManager.INITIAL_HASH)
        val boundaryAnchorHash = fullChain[2].entryHash
        val retainedChain = fullChain.subList(3, 10).toMutableList()

        // Attacker tries to alter prevHash of the first retained event and recomputes entryHash
        val fakePrevHash = "a1b2c3d4e5f67890a1b2c3d4e5f67890a1b2c3d4e5f67890a1b2c3d4e5f67890"
        val tamperedFirst = createHelperEvent(
            id = retainedChain[0].id,
            timestampMs = retainedChain[0].timestampMs,
            type = retainedChain[0].type,
            payloadJson = retainedChain[0].payloadJson,
            prevHash = fakePrevHash
        )
        retainedChain[0] = tamperedFirst

        assertFalse(
            "Changing prevHash of first retained event away from boundary anchor must cause verification failure",
            HashChainManager.verifyChainIntegrity(retainedChain, expectedBoundaryHash = boundaryAnchorHash)
        )
    }

    @Test
    fun testDeletedFirstRetainedEventDetected() {
        val fullChain = generateSampleChain(10, HashChainManager.INITIAL_HASH)
        val boundaryAnchorHash = fullChain[2].entryHash // Expected prevHash for retained index 3
        val retainedChain = fullChain.subList(3, 10).toMutableList()

        // Attacker deletes the first retained event (index 0 of retainedChain, index 3 of fullChain)
        retainedChain.removeAt(0)

        assertFalse(
            "Deleting the first retained event must fail verification because new first event's prevHash doesn't match boundary anchor",
            HashChainManager.verifyChainIntegrity(retainedChain, expectedBoundaryHash = boundaryAnchorHash)
        )
    }

    @Test
    fun testInsertedFirstEventDetected() {
        val fullChain = generateSampleChain(10, HashChainManager.INITIAL_HASH)
        val boundaryAnchorHash = fullChain[2].entryHash
        val retainedChain = fullChain.subList(3, 10).toMutableList()

        // Attacker inserts an unauthorized event at the front of the retained chain
        val insertedEvent = createHelperEvent(
            id = 999L,
            timestampMs = retainedChain[0].timestampMs - 100L,
            type = EventType.ACCEL,
            payloadJson = """{"insertedAtFront":true}""",
            prevHash = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
        )
        retainedChain.add(0, insertedEvent)

        assertFalse(
            "Inserting an unauthorized event at the front must fail verification against boundary anchor",
            HashChainManager.verifyChainIntegrity(retainedChain, expectedBoundaryHash = boundaryAnchorHash)
        )
    }

    @Test
    fun testModifiedMiddleEventDetected() {
        val fullChain = generateSampleChain(10, HashChainManager.INITIAL_HASH)
        val boundaryAnchorHash = fullChain[2].entryHash
        val retainedChain = fullChain.subList(3, 10).toMutableList()

        // Attacker modifies a middle event (index 3 of retainedChain)
        val tamperedMiddle = retainedChain[3].copy(payloadJson = """{"middleTampered":true}""")
        retainedChain[3] = tamperedMiddle

        assertFalse(
            "Modifying a middle event must fail chain integrity verification",
            HashChainManager.verifyChainIntegrity(retainedChain, expectedBoundaryHash = boundaryAnchorHash)
        )
    }

    @Test
    fun testDeletedMiddleEventDetected() {
        val fullChain = generateSampleChain(10, HashChainManager.INITIAL_HASH)
        val boundaryAnchorHash = fullChain[2].entryHash
        val retainedChain = fullChain.subList(3, 10).toMutableList()

        // Attacker deletes a middle event
        retainedChain.removeAt(2)

        assertFalse(
            "Deleting a middle event must fail chain continuity verification",
            HashChainManager.verifyChainIntegrity(retainedChain, expectedBoundaryHash = boundaryAnchorHash)
        )
    }

    @Test
    fun testModifiedFinalEventDetected() {
        val fullChain = generateSampleChain(10, HashChainManager.INITIAL_HASH)
        val boundaryAnchorHash = fullChain[2].entryHash
        val retainedChain = fullChain.subList(3, 10).toMutableList()

        // Attacker modifies the final event
        val lastIdx = retainedChain.size - 1
        val tamperedLast = retainedChain[lastIdx].copy(payloadJson = """{"lastTampered":true}""")
        retainedChain[lastIdx] = tamperedLast

        assertFalse(
            "Modifying the final event must fail integrity verification",
            HashChainManager.verifyChainIntegrity(retainedChain, expectedBoundaryHash = boundaryAnchorHash)
        )
    }

    @Test
    fun testMerkleRootHashReproducibility() {
        val chain = generateSampleChain(5, HashChainManager.INITIAL_HASH)
        val hashes = chain.map { it.entryHash }

        val root1 = HashChainManager.computeChainRootHash(hashes)
        val root2 = HashChainManager.computeChainRootHash(hashes)

        assertEquals("Merkle root hash calculation must be deterministic and reproducible", root1, root2)
    }
}
