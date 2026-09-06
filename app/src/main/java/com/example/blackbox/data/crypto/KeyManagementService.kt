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

/**
 * Key Management Service for client-side envelope encryption,
 * Android Keystore asymmetric digital signing, and encrypted Medical ID storage.
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
     * Returns Pair(signatureBase64, publicKeyBase64).
     */
    fun signData(data: String): Pair<String, String> {
        val (privateKey, publicKey) = getOrCreateAsymmetricKeyPair()
        val signature = Signature.getInstance("SHA256withRSA").apply {
            initSign(privateKey)
            update(data.toByteArray(Charsets.UTF_8))
        }
        val sigBytes = signature.sign()

        val sigBase64 = Base64.encodeToString(sigBytes, Base64.NO_WRAP)
        val pubKeyBase64 = Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
        return Pair(sigBase64, pubKeyBase64)
    }

    /**
     * Verifies digital signature using stored Public Key (Feature 2.2).
     */
    fun verifySignature(data: String, signatureBase64: String, publicKeyBase64: String): Boolean {
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
     * Encrypts plaintext string using AES-256-GCM with a newly generated symmetric key.
     */
    fun encryptIncidentBundle(plainText: String): Pair<String, String> {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        val secretKey = keyGen.generateKey()

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv

        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        val combined = ByteArray(iv.size + cipherBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherBytes, 0, combined, iv.size, cipherBytes.size)

        val encryptedBase64 = Base64.encodeToString(combined, Base64.NO_WRAP)
        val keyBase64 = Base64.encodeToString(secretKey.encoded, Base64.NO_WRAP)

        return Pair(encryptedBase64, keyBase64)
    }

    /**
     * Encrypted Storage for Emergency Medical ID & Contact Data (Feature 2.5).
     */
    fun saveMedicalIdData(medicalId: MedicalIdData) {
        encryptedPrefs.edit()
            .putString(KEY_MEDICAL_BLOOD, medicalId.bloodGroup)
            .putString(KEY_MEDICAL_ALLERGIES, medicalId.allergies)
            .putString(KEY_MEDICAL_NOTES, medicalId.medicalNotes)
            .putString(KEY_MEDICAL_CONTACT_NAME, medicalId.emergencyContactName)
            .putString(KEY_MEDICAL_CONTACT_PHONE, medicalId.emergencyContactPhone)
            .apply()
    }

    fun getMedicalIdData(): MedicalIdData {
        return MedicalIdData(
            bloodGroup = encryptedPrefs.getString(KEY_MEDICAL_BLOOD, "Not Specified") ?: "Not Specified",
            allergies = encryptedPrefs.getString(KEY_MEDICAL_ALLERGIES, "None Known") ?: "None Known",
            medicalNotes = encryptedPrefs.getString(KEY_MEDICAL_NOTES, "No critical pre-existing conditions reported") ?: "No critical pre-existing conditions reported",
            emergencyContactName = encryptedPrefs.getString(KEY_MEDICAL_CONTACT_NAME, "") ?: "",
            emergencyContactPhone = encryptedPrefs.getString(KEY_MEDICAL_CONTACT_PHONE, "") ?: ""
        )
    }

    companion object {
        private const val KEY_DB_PASSPHRASE = "key_db_passphrase"
        private const val KEY_ALIAS_ASYMMETRIC = "trace_asymmetric_key"

        private const val KEY_MEDICAL_BLOOD = "key_medical_blood"
        private const val KEY_MEDICAL_ALLERGIES = "key_medical_allergies"
        private const val KEY_MEDICAL_NOTES = "key_medical_notes"
        private const val KEY_MEDICAL_CONTACT_NAME = "key_medical_contact_name"
        private const val KEY_MEDICAL_CONTACT_PHONE = "key_medical_contact_phone"
    }
}
