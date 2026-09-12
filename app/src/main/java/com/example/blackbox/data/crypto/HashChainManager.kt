package com.example.blackbox.data.crypto

import com.example.blackbox.data.db.SensorEvent
import java.security.MessageDigest

/**
 * Manages cryptographic hash-chaining for tamper-evident sensor logging.
 * Each buffer entry incorporates the previous entry's hash:
 * entryHash = SHA-256(prevHash + payloadJson + timestampMs)
 */
object HashChainManager {

    const val INITIAL_HASH = "0000000000000000000000000000000000000000000000000000000000000000"

    fun computeEntryHash(prevHash: String, payloadJson: String, timestampMs: Long): String {
        val input = "$prevHash|$payloadJson|$timestampMs"
        return sha256(input)
    }

    /**
     * Computes a Merkle-style root hash over a list of event hashes.
     */
    fun computeChainRootHash(hashes: List<String>): String {
        if (hashes.isEmpty()) return sha256("EMPTY_CHAIN")
        var currentLevel = hashes
        while (currentLevel.size > 1) {
            val nextLevel = mutableListOf<String>()
            for (i in currentLevel.indices step 2) {
                if (i + 1 < currentLevel.size) {
                    nextLevel.add(sha256(currentLevel[i] + currentLevel[i + 1]))
                } else {
                    nextLevel.add(sha256(currentLevel[i] + currentLevel[i]))
                }
            }
            currentLevel = nextLevel
        }
        return currentLevel.first()
    }

    /**
     * Verifies that every link in the hash chain is mathematically intact.
     *
     * For partial or pruned rolling buffer windows, the verification treats
     * the first retained event's prevHash as the anchored boundary and validates
     * cryptographic continuity and payload integrity for all subsequent events.
     *
     * @return true if no entry was modified, inserted, or removed post-hoc.
     */
    fun verifyChainIntegrity(events: List<SensorEvent>): Boolean {
        if (events.isEmpty()) return true

        // Treat the first retained event's prevHash as the anchored boundary
        var expectedPrevHash = events.first().prevHash

        for (event in events) {
            // 1. Verify link continuity
            if (event.prevHash != expectedPrevHash) {
                return false
            }
            // 2. Re-compute SHA-256 hash over prevHash, payloadJson, and timestampMs
            val calculatedHash = computeEntryHash(event.prevHash, event.payloadJson, event.timestampMs)
            if (event.entryHash != calculatedHash) {
                return false
            }
            // 3. Advance expected prevHash
            expectedPrevHash = event.entryHash
        }
        return true
    }

    fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
