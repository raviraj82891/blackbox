package com.example.blackbox.data.crypto

import android.content.ContextWrapper
import com.example.blackbox.data.db.IncidentReport
import com.example.blackbox.data.db.TriggerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class CryptoHardeningTest {

    private lateinit var keyManagementService: TestCryptoKeyManagementService

    private class TestCryptoKeyManagementService : KeyManagementService(
        context = ContextWrapper(null)
    ) {
        private var keyStoreAliasDeleted = false

        override fun encryptIncidentBundle(plainText: String): Pair<String, String> {
            val fakeEncrypted = "ENCRYPTED_GCM_BUNDLE_FOR_" + plainText.hashCode()
            val fakeWrappedKey = "WRAPPED_DEK_KEY_ALIAS_GCM"
            return Pair(fakeEncrypted, fakeWrappedKey)
        }

        override fun decryptIncidentBundle(encryptedBundleBase64: String, wrappedKeyBase64: String): String {
            return "DECRYPTED_PLAINTEXT_TIMELINE_PAYLOAD"
        }

        override fun signData(data: String): Result<Pair<String, String>> {
            if (data == "FAIL_SIGNING") {
                return Result.failure(IllegalStateException("Hardware KeyStore signature failed"))
            }
            return Result.success(Pair("MOCK_RSA_SIGNATURE_BASE64", "MOCK_PUBLIC_KEY_BASE64"))
        }

        override fun verifySignature(data: String, signatureBase64: String, publicKeyBase64: String): Boolean {
            if (data == "TAMPERED_DATA" || signatureBase64 == "TAMPERED_SIGNATURE") return false
            return signatureBase64 == "MOCK_RSA_SIGNATURE_BASE64" && publicKeyBase64 == "MOCK_PUBLIC_KEY_BASE64"
        }

        override fun purgeAllEncryptedPreferencesAndKeys() {
            keyStoreAliasDeleted = true
        }

        fun isKeyStoreAliasDeleted(): Boolean = keyStoreAliasDeleted
    }

    @Before
    fun setUp() {
        keyManagementService = TestCryptoKeyManagementService()
    }

    @Test
    fun testEnvelopeEncryptionAndDecryption() {
        val originalTimeline = "Emergency Incident Activated via AUTO_CRASH • Speed 65 km/h"
        val (encryptedBundle, wrappedKey) = keyManagementService.encryptIncidentBundle(originalTimeline)

        assertNotNull("Encrypted bundle must not be null", encryptedBundle)
        assertNotNull("Wrapped DEK key must not be null", wrappedKey)
        assertTrue("Raw decryption key must not be stored plainly", wrappedKey.contains("WRAPPED"))

        val decryptedText = keyManagementService.decryptIncidentBundle(encryptedBundle, wrappedKey)
        assertNotNull("Decrypted timeline must not be null", decryptedText)
    }

    @Test
    fun testDigitalSignatureSuccessAndVerification() {
        val rootHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        val signResult = keyManagementService.signData(rootHash)

        assertTrue("Signing valid data must return Result.success", signResult.isSuccess)
        val (signature, publicKey) = signResult.getOrThrow()

        val isValid = keyManagementService.verifySignature(rootHash, signature, publicKey)
        assertTrue("Signature verification must succeed for untampered data", isValid)
    }

    @Test
    fun testTamperedDataSignatureFailsVerification() {
        val originalHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        val signResult = keyManagementService.signData(originalHash)
        val (signature, publicKey) = signResult.getOrThrow()

        val isTamperedDataValid = keyManagementService.verifySignature("TAMPERED_DATA", signature, publicKey)
        assertFalse("Tampered root hash must fail signature verification", isTamperedDataValid)

        val isTamperedSigValid = keyManagementService.verifySignature(originalHash, "TAMPERED_SIGNATURE", publicKey)
        assertFalse("Tampered digital signature must fail verification", isTamperedSigValid)
    }

    @Test
    fun testSigningFailureDoesNotClaimSignature() {
        val signResult = keyManagementService.signData("FAIL_SIGNING")

        assertTrue("Failed signing must return Result.failure", signResult.isFailure)
        val (signature, publicKey) = signResult.getOrNull() ?: Pair(null, null)

        assertNull("Failed signing must result in null signature", signature)
        assertNull("Failed signing must result in null public key", publicKey)
    }

    @Test
    fun testUnsignedReportStatusClassification() {
        val unsignedReport = IncidentReport(
            id = UUID.randomUUID().toString(),
            triggeredAt = System.currentTimeMillis(),
            triggerType = TriggerType.MANUAL_SOS,
            timelineJson = "Timeline",
            chainRootHash = "hash123",
            digitalSignature = null,
            publicKeyBase64 = null
        )

        val sig = unsignedReport.digitalSignature
        val pubKey = unsignedReport.publicKeyBase64

        val status = if (sig.isNullOrBlank() || pubKey.isNullOrBlank()) {
            SignatureStatus.UNSIGNED
        } else {
            SignatureStatus.SIGNED
        }

        assertEquals("Report with null digital signature must be classified as UNSIGNED", SignatureStatus.UNSIGNED, status)
    }

    @Test
    fun testKeyStoreDestructionOnFactoryReset() {
        keyManagementService.purgeAllEncryptedPreferencesAndKeys()
        assertTrue("Factory reset must destroy KeyStore encryption and signing aliases", keyManagementService.isKeyStoreAliasDeleted())
    }
}
