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
    fun testIntactCompleteChain() {
        val chain = generateSampleChain(10, HashChainManager.INITIAL_HASH)
        assertTrue("Intact complete chain starting from INITIAL_HASH should pass verification", HashChainManager.verifyChainIntegrity(chain))
    }

    @Test
    fun testIntactPartialChain() {
        val fullChain = generateSampleChain(10, HashChainManager.INITIAL_HASH)
        // Partial chain from index 3 to 8
        val partialChain = fullChain.subList(3, 8)
        assertTrue("Intact partial chain starting with non-initial prevHash should pass verification using boundary anchor", HashChainManager.verifyChainIntegrity(partialChain))
    }

    @Test
    fun testDeletedOldEvents() {
        val fullChain = generateSampleChain(15, HashChainManager.INITIAL_HASH)
        // Simulate retention pruning of events 1..5, retaining events 6..15
        val retainedEvents = fullChain.subList(5, 15)
        assertTrue("Retained events after pruning old events should verify successfully against boundary anchor", HashChainManager.verifyChainIntegrity(retainedEvents))
    }

    @Test
    fun testModifiedPayload() {
        val chain = generateSampleChain(10, HashChainManager.INITIAL_HASH).toMutableList()
        val tamperedEvent = chain[4].copy(payloadJson = """{"eventIndex":5,"magnitude":999.0}""")
        chain[4] = tamperedEvent

        assertFalse("Chain with tampered event payload should fail verification", HashChainManager.verifyChainIntegrity(chain))
    }

    @Test
    fun testModifiedTimestamp() {
        val chain = generateSampleChain(10, HashChainManager.INITIAL_HASH).toMutableList()
        val tamperedEvent = chain[3].copy(timestampMs = 1700000000000L)
        chain[3] = tamperedEvent

        assertFalse("Chain with tampered event timestamp should fail verification", HashChainManager.verifyChainIntegrity(chain))
    }

    @Test
    fun testModifiedPrevHash() {
        val chain = generateSampleChain(10, HashChainManager.INITIAL_HASH).toMutableList()
        val tamperedEvent = chain[2].copy(prevHash = "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef")
        chain[2] = tamperedEvent

        assertFalse("Chain with tampered prevHash should fail verification", HashChainManager.verifyChainIntegrity(chain))
    }

    @Test
    fun testInsertedEvent() {
        val chain = generateSampleChain(10, HashChainManager.INITIAL_HASH).toMutableList()
        val insertedEvent = createHelperEvent(
            id = 999L,
            timestampMs = chain[4].timestampMs + 500L,
            type = EventType.ACCEL,
            payloadJson = """{"inserted":true}""",
            prevHash = chain[3].entryHash
        )
        chain.add(4, insertedEvent)

        assertFalse("Chain with unauthorized inserted event should fail verification", HashChainManager.verifyChainIntegrity(chain))
    }

    @Test
    fun testRemovedEvent() {
        val chain = generateSampleChain(10, HashChainManager.INITIAL_HASH).toMutableList()
        // Remove event at index 4
        chain.removeAt(4)

        assertFalse("Chain with missing event removed from middle should fail verification", HashChainManager.verifyChainIntegrity(chain))
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
