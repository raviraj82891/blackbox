package com.example.blackbox.data.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class MedicalIdData(
    val bloodGroup: String = "Not Specified",
    val allergies: String = "None Known",
    val medicalNotes: String = "No critical pre-existing conditions reported",
    val emergencyContactName: String = "",
    val emergencyContactPhone: String = ""
)

data class BufferAnchor(
    val boundaryTimestampMs: Long = 0L,
    val boundaryEntryHash: String = HashChainManager.INITIAL_HASH,
    val totalPurgedCount: Long = 0L
)

enum class SignatureStatus {
    SIGNED,
    UNSIGNED,
    SIGNATURE_INVALID
}

/**
 * Key Management Service for client-side envelope encryption,
 * Android Keystore key wrapping, asymmetric digital signing,
 * session auth tokens, and encrypted storage.
 */
open class KeyManagementService(private val context: Context) {

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedPrefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            "blackbox_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Session Auth Token Storage (EncryptedSharedPreferences).
     */
    open fun getAuthToken(): String? {
        return encryptedPrefs.getString(KEY_AUTH_TOKEN, null)
    }

    open fun saveAuthToken(token: String) {
        encryptedPrefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
    }

    open fun clearAuthSession() {
        encryptedPrefs.edit().remove(KEY_AUTH_TOKEN).apply()
    }

    /**
     * Completely purges all encrypted preferences, calibration parameters, analytics counters,
     * medical ID data, boundary anchors, onboarding completion state, and keystore key aliases.
     */
    open fun purgeAllEncryptedPreferencesAndKeys() {
        encryptedPrefs.edit().clear().commit()

        runCatching {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (keyStore.containsAlias(KEY_ALIAS_ASYMMETRIC)) {
                keyStore.deleteEntry(KEY_ALIAS_ASYMMETRIC)
            }
            if (keyStore.containsAlias(KEY_ALIAS_WRAPPER)) {
                keyStore.deleteEntry(KEY_ALIAS_WRAPPER)
            }
        }
    }

    /**
     * Permanent storage for onboarding completion status.
     */
    open fun isOnboardingCompleted(): Boolean {
        return encryptedPrefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    open fun setOnboardingCompleted(completed: Boolean) {
        encryptedPrefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    /**
     * Storage for Automatic Crash & Fall Detection Enablement.
     */
    open fun isAutoDetectionEnabled(): Boolean {
        return encryptedPrefs.getBoolean(KEY_AUTO_DETECTION_ENABLED, true)
    }

    open fun setAutoDetectionEnabled(enabled: Boolean) {
        encryptedPrefs.edit().putBoolean(KEY_AUTO_DETECTION_ENABLED, enabled).apply()
    }

    /**
     * Storage for Personal Motion Calibration Results.
     */
    open fun saveCalibrationResult(meanMagnitude: Double, threshold: Double, timestampMs: Long = System.currentTimeMillis()) {
        encryptedPrefs.edit()
            .putFloat(KEY_CALIBRATION_MEAN, meanMagnitude.toFloat())
            .putFloat(KEY_CALIBRATION_THRESHOLD, threshold.toFloat())
            .putLong(KEY_CALIBRATION_TIMESTAMP, timestampMs)
            .apply()
    }

    open fun getLastCalibrationSummary(): String? {
        val timestamp = encryptedPrefs.getLong(KEY_CALIBRATION_TIMESTAMP, 0L)
        if (timestamp == 0L) return null
        val mean = encryptedPrefs.getFloat(KEY_CALIBRATION_MEAN, 9.81f)
        val threshold = encryptedPrefs.getFloat(KEY_CALIBRATION_THRESHOLD, 20.0f)
        return "Last Calibrated: %.1f m/s² baseline • Threshold set to %.1f m/s²".format(mean, threshold)
    }

    /**
     * Local Privacy-First Analytics Counter Storage.
     */
    open fun getCancelledCountdownsCount(): Int {
        return encryptedPrefs.getInt(KEY_CANCELLED_COUNTDOWNS, 0)
    }

    open fun incrementCancelledCountdowns() {
        val current = getCancelledCountdownsCount()
        encryptedPrefs.edit().putInt(KEY_CANCELLED_COUNTDOWNS, current + 1).apply()
    }

    open fun getSafetyCheckInCount(): Int {
        return encryptedPrefs.getInt(KEY_SAFETY_CHECKIN_COUNT, 0)
    }

    open fun incrementSafetyCheckInCount() {
        val current = getSafetyCheckInCount()
        encryptedPrefs.edit().putInt(KEY_SAFETY_CHECKIN_COUNT, current + 1).apply()
    }

    /**
     * Cryptographic Boundary Anchor Storage (EncryptedSharedPreferences).
     * Anchors the rolling buffer boundary hash to detect deletion/tampering of the first retained event.
     */
    open fun getBufferAnchor(): BufferAnchor {
        val timestamp = encryptedPrefs.getLong(KEY_ANCHOR_TIMESTAMP, 0L)
        val hash = encryptedPrefs.getString(KEY_ANCHOR_HASH, HashChainManager.INITIAL_HASH) ?: HashChainManager.INITIAL_HASH
        val count = encryptedPrefs.getLong(KEY_ANCHOR_PURGED_COUNT, 0L)
        return BufferAnchor(timestamp, hash, count)
    }

    open fun saveBufferAnchor(anchor: BufferAnchor) {
        encryptedPrefs.edit()
            .putLong(KEY_ANCHOR_TIMESTAMP, anchor.boundaryTimestampMs)
            .putString(KEY_ANCHOR_HASH, anchor.boundaryEntryHash)
            .putLong(KEY_ANCHOR_PURGED_COUNT, anchor.totalPurgedCount)
            .apply()
    }

    open fun clearBufferAnchor() {
        encryptedPrefs.edit()
            .remove(KEY_ANCHOR_TIMESTAMP)
            .remove(KEY_ANCHOR_HASH)
            .remove(KEY_ANCHOR_PURGED_COUNT)
            .apply()
    }

    /**
     * Gets or generates the database encryption passphrase for SQLCipher.
     */
    open fun getOrCreateDatabasePassphrase(): String {
        val existing = encryptedPrefs.getString(KEY_DB_PASSPHRASE, null)
        if (existing != null) return existing

        val randomBytes = ByteArray(32)
        SecureRandom().nextBytes(randomBytes)
        val passphrase = Base64.encodeToString(randomBytes, Base64.NO_WRAP)
        encryptedPrefs.edit().putString(KEY_DB_PASSPHRASE, passphrase).apply()
        return passphrase
    }

    /**
     * Keystore Master Key for DEK Envelope Key Wrapping.
     */
    private fun getOrCreateWrapperKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS_WRAPPER)) {
            val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS_WRAPPER,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGen.init(spec)
            keyGen.generateKey()
        }
        val entry = keyStore.getEntry(KEY_ALIAS_WRAPPER, null) as KeyStore.SecretKeyEntry
        return entry.secretKey
    }

    /**
     * Wraps a per-incident AES-256 Data Encryption Key (DEK) using the Keystore Master Key.
     */
    open fun wrapDataEncryptionKey(dek: SecretKey): String {
        val wrapperKey = getOrCreateWrapperKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, wrapperKey)
        val iv = cipher.iv
        val wrappedBytes = cipher.doFinal(dek.encoded)

        val combined = ByteArray(iv.size + wrappedBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(wrappedBytes, 0, combined, iv.size, wrappedBytes.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /**
     * Unwraps a wrapped DEK using the Keystore Master Key.
     */
    open fun unwrapDataEncryptionKey(wrappedKeyBase64: String): SecretKey {
        val combined = Base64.decode(wrappedKeyBase64, Base64.NO_WRAP)
        val wrapperKey = getOrCreateWrapperKey()

        val iv = ByteArray(12)
        val wrappedBytes = ByteArray(combined.size - 12)
        System.arraycopy(combined, 0, iv, 0, 12)
        System.arraycopy(combined, 12, wrappedBytes, 0, wrappedBytes.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, wrapperKey, gcmSpec)
        val rawDek = cipher.doFinal(wrappedBytes)
        return SecretKeySpec(rawDek, "AES")
    }

    /**
     * Encrypts incident payload using a newly generated per-incident AES-256 DEK.
     * The DEK itself is wrapped with the Keystore Master Key.
     * Returns Pair(encryptedBundleBase64, wrappedKeyBase64).
     */
    open fun encryptIncidentBundle(plainText: String): Pair<String, String> {
        val keyGen = KeyGenerator.getInstance("AES").apply { init(256) }
        val dek = keyGen.generateKey()

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, dek)
        val iv = cipher.iv
        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        val combined = ByteArray(iv.size + cipherBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherBytes, 0, combined, iv.size, cipherBytes.size)

        val encryptedBase64 = Base64.encodeToString(combined, Base64.NO_WRAP)
        val wrappedKeyBase64 = wrapDataEncryptionKey(dek)

        return Pair(encryptedBase64, wrappedKeyBase64)
    }

    /**
     * Decrypts incident payload locally using the unwrapped per-incident DEK.
     */
    open fun decryptIncidentBundle(encryptedBundleBase64: String, wrappedKeyBase64: String): String {
        val dek = unwrapDataEncryptionKey(wrappedKeyBase64)
        val combined = Base64.decode(encryptedBundleBase64, Base64.NO_WRAP)

        val iv = ByteArray(12)
        val cipherBytes = ByteArray(combined.size - 12)
        System.arraycopy(combined, 0, iv, 0, 12)
        System.arraycopy(combined, 12, cipherBytes, 0, cipherBytes.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, dek, gcmSpec)
        val plainBytes = cipher.doFinal(cipherBytes)
        return String(plainBytes, Charsets.UTF_8)
    }

    /**
     * Asymmetric Hardware-backed RSA Key Pair in Android Keystore (Feature 2.2).
     */
    private fun getOrCreateAsymmetricKeyPair(): Pair<PrivateKey, PublicKey> {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS_ASYMMETRIC)) {
            val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")
            val parameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS_ASYMMETRIC,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            )
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                .setKeySize(2048)
                .build()
            kpg.initialize(parameterSpec)
            kpg.generateKeyPair()
        }

        val entry = keyStore.getEntry(KEY_ALIAS_ASYMMETRIC, null) as KeyStore.PrivateKeyEntry
        return Pair(entry.privateKey, entry.certificate.publicKey)
    }

    /**
     * Signs data (e.g. Merkle Chain Root Hash) using Keystore Private Key.
     * Returns Result containing Pair(signatureBase64, publicKeyBase64).
     */
    open fun signData(data: String): Result<Pair<String, String>> {
        return runCatching {
            val (privateKey, publicKey) = getOrCreateAsymmetricKeyPair()
            val signature = Signature.getInstance("SHA256withRSA").apply {
                initSign(privateKey)
                update(data.toByteArray(Charsets.UTF_8))
            }
            val sigBytes = signature.sign()

            val sigBase64 = Base64.encodeToString(sigBytes, Base64.NO_WRAP)
            val pubKeyBase64 = Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
            Pair(sigBase64, pubKeyBase64)
        }
    }

    /**
     * Verifies digital signature using stored Public Key (Feature 2.2).
     */
    open fun verifySignature(data: String, signatureBase64: String, publicKeyBase64: String): Boolean {
        return try {
            val pubKeyBytes = Base64.decode(publicKeyBase64, Base64.NO_WRAP)
            val keySpec = X509EncodedKeySpec(pubKeyBytes)
            val keyFactory = KeyFactory.getInstance("RSA")
            val publicKey = keyFactory.generatePublic(keySpec)

            val sigBytes = Base64.decode(signatureBase64, Base64.NO_WRAP)
            val signature = Signature.getInstance("SHA256withRSA").apply {
                initVerify(publicKey)
                update(data.toByteArray(Charsets.UTF_8))
            }
            signature.verify(sigBytes)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Encrypted Storage for Emergency Medical ID & Contact Data (Feature 2.5).
     */
    open fun saveMedicalIdData(medicalId: MedicalIdData) {
        encryptedPrefs.edit()
            .putString(KEY_MEDICAL_BLOOD, medicalId.bloodGroup)
            .putString(KEY_MEDICAL_ALLERGIES, medicalId.allergies)
            .putString(KEY_MEDICAL_NOTES, medicalId.medicalNotes)
            .putString(KEY_MEDICAL_CONTACT_NAME, medicalId.emergencyContactName)
            .putString(KEY_MEDICAL_CONTACT_PHONE, medicalId.emergencyContactPhone)
            .apply()
    }

    open fun getMedicalIdData(): MedicalIdData {
        return MedicalIdData(
            bloodGroup = encryptedPrefs.getString(KEY_MEDICAL_BLOOD, "Not Specified") ?: "Not Specified",
            allergies = encryptedPrefs.getString(KEY_MEDICAL_ALLERGIES, "None Known") ?: "None Known",
            medicalNotes = encryptedPrefs.getString(KEY_MEDICAL_NOTES, "No critical pre-existing conditions reported") ?: "No critical pre-existing conditions reported",
            emergencyContactName = encryptedPrefs.getString(KEY_MEDICAL_CONTACT_NAME, "") ?: "",
            emergencyContactPhone = encryptedPrefs.getString(KEY_MEDICAL_CONTACT_PHONE, "") ?: ""
        )
    }

    companion object {
        private const val KEY_AUTH_TOKEN = "key_auth_token"
        private const val KEY_ONBOARDING_COMPLETED = "key_onboarding_completed"
        private const val KEY_AUTO_DETECTION_ENABLED = "key_auto_detection_enabled"
        private const val KEY_DB_PASSPHRASE = "key_db_passphrase"
        private const val KEY_ALIAS_ASYMMETRIC = "trace_asymmetric_key"
        private const val KEY_ALIAS_WRAPPER = "trace_dek_wrapper_key"
        private const val KEY_CANCELLED_COUNTDOWNS = "key_cancelled_countdowns"
        private const val KEY_SAFETY_CHECKIN_COUNT = "key_safety_checkin_count"

        private const val KEY_MEDICAL_BLOOD = "key_medical_blood"
        private const val KEY_MEDICAL_ALLERGIES = "key_medical_allergies"
        private const val KEY_MEDICAL_NOTES = "key_medical_notes"
        private const val KEY_MEDICAL_CONTACT_NAME = "key_medical_contact_name"
        private const val KEY_MEDICAL_CONTACT_PHONE = "key_medical_contact_phone"

        private const val KEY_CALIBRATION_MEAN = "key_calibration_mean"
        private const val KEY_CALIBRATION_THRESHOLD = "key_calibration_threshold"
        private const val KEY_CALIBRATION_TIMESTAMP = "key_calibration_timestamp"

        private const val KEY_ANCHOR_TIMESTAMP = "key_anchor_timestamp"
        private const val KEY_ANCHOR_HASH = "key_anchor_hash"
        private const val KEY_ANCHOR_PURGED_COUNT = "key_anchor_purged_count"
    }
}
