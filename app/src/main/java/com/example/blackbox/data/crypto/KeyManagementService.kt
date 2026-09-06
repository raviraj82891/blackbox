package com.example.blackbox.data.crypto

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

/**
 * Key Management Service for client-side envelope encryption and Android Keystore integration.
 * Ensures the zero-knowledge principle: incident bundles are encrypted client-side using AES-256-GCM
 * before uploading to AWS S3. The backend never holds the plaintext or decryption keys.
 */
class KeyManagementService(private val context: Context) {

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
     * Gets or generates the database encryption passphrase for SQLCipher.
     */
    fun getOrCreateDatabasePassphrase(): String {
        val existing = encryptedPrefs.getString(KEY_DB_PASSPHRASE, null)
        if (existing != null) return existing

        val randomBytes = ByteArray(32)
        SecureRandom().nextBytes(randomBytes)
        val passphrase = Base64.encodeToString(randomBytes, Base64.NO_WRAP)
        encryptedPrefs.edit().putString(KEY_DB_PASSPHRASE, passphrase).apply()
        return passphrase
    }

    /**
     * Encrypts plaintext string using AES-256-GCM with a newly generated symmetric key.
     * Returns a pair of (ciphertextWithIvInBase64, rawKeyInBase64).
     */
    fun encryptIncidentBundle(plainText: String): Pair<String, String> {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        val secretKey = keyGen.generateKey()

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv

        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        // Prepend 12-byte IV to ciphertext
        val combined = ByteArray(iv.size + cipherBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherBytes, 0, combined, iv.size, cipherBytes.size)

        val encryptedBase64 = Base64.encodeToString(combined, Base64.NO_WRAP)
        val keyBase64 = Base64.encodeToString(secretKey.encoded, Base64.NO_WRAP)

        return Pair(encryptedBase64, keyBase64)
    }

    /**
     * Decrypts ciphertext in Base64 using key in Base64 (for testing/contact decryption).
     */
    fun decryptIncidentBundle(encryptedBase64: String, keyBase64: String): String {
        val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
        val rawKey = Base64.decode(keyBase64, Base64.NO_WRAP)
        val secretKey: SecretKey = SecretKeySpec(rawKey, "AES")

        val iv = ByteArray(12)
        System.arraycopy(combined, 0, iv, 0, 12)

        val cipherBytes = ByteArray(combined.size - 12)
        System.arraycopy(combined, 12, cipherBytes, 0, cipherBytes.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        val decryptedBytes = cipher.doFinal(cipherBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    companion object {
        private const val KEY_DB_PASSPHRASE = "key_db_passphrase"
    }
}
