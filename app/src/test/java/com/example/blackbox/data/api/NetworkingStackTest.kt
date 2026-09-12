package com.example.blackbox.data.api

import okhttp3.Call
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.TimeUnit

class NetworkingStackTest {

    @Test
    fun testSanitizedLoggingRedactsSensitiveData() {
        val loggedMessages = mutableListOf<String>()
        val interceptor = SanitizedLoggingInterceptor(
            isDebug = true,
            logger = { loggedMessages.add(it) }
        )

        val rawInput = """
            {
                "token": "secret_jwt_token_xyz",
                "emergencyContactPhone": "+15551234567",
                "email": "user@example.com",
                "encryptedBundle": "AES_GCM_ENCRYPTED_TELEMETRY_DATA",
                "decryptionKey": "SECRET_AES_256_KEY",
                "allergies": "Severe Penicillin Allergy",
                "bloodGroup": "O-Positive",
                "medicalNotes": "Diabetic Type 1"
            }
        """.trimIndent()

        val sanitized = interceptor.sanitizeText(rawInput)

        // Verify no raw PII or secret keys remain
        assertFalse(sanitized.contains("secret_jwt_token_xyz"))
        assertFalse(sanitized.contains("+15551234567"))
        assertFalse(sanitized.contains("user@example.com"))
        assertFalse(sanitized.contains("AES_GCM_ENCRYPTED_TELEMETRY_DATA"))
        assertFalse(sanitized.contains("SECRET_AES_256_KEY"))
        assertFalse(sanitized.contains("Severe Penicillin Allergy"))

        // Verify redaction tags are present
        assertTrue(sanitized.contains("[REDACTED_TOKEN]"))
        assertTrue(sanitized.contains("[REDACTED_PHONE]"))
        assertTrue(sanitized.contains("[REDACTED_EMAIL]"))
        assertTrue(sanitized.contains("[REDACTED_PAYLOAD]"))
        assertTrue(sanitized.contains("[REDACTED_KEY]"))
        assertTrue(sanitized.contains("[REDACTED_MEDICAL]"))
    }

    @Test
    fun testEmergencyRetryInterceptorRetriesOn500ServerError() {
        var callCount = 0

        val fakeChain = object : Interceptor.Chain {
            override fun request(): Request = Request.Builder().url("https://api.blackbox-safety.aws/v1/incidents").build()

            override fun proceed(request: Request): Response {
                callCount++
                val code = if (callCount < 3) 500 else 200
                return Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(code)
                    .message(if (code == 200) "OK" else "Server Error")
                    .body("""{"status":"OK"}""".toResponseBody("application/json".toMediaType()))
                    .build()
            }

            override fun connection(): Connection? = null
            override fun call(): Call = throw UnsupportedOperationException()
            override fun connectTimeoutMillis(): Int = 1000
            override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
            override fun readTimeoutMillis(): Int = 1000
            override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
            override fun writeTimeoutMillis(): Int = 1000
            override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        }

        val interceptor = EmergencyRetryInterceptor(maxRetries = 3, initialBackoffMs = 1L)
        val response = interceptor.intercept(fakeChain)

        assertEquals(200, response.code)
        assertEquals(3, callCount)
    }

    @Test
    fun testEmergencyRetryInterceptorRetriesOnIOException() {
        var callCount = 0

        val fakeChain = object : Interceptor.Chain {
            override fun request(): Request = Request.Builder().url("https://api.blackbox-safety.aws/v1/incidents").build()

            override fun proceed(request: Request): Response {
                callCount++
                if (callCount < 2) {
                    throw IOException("Network connection dropped")
                }
                return Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("""{"status":"OK"}""".toResponseBody("application/json".toMediaType()))
                    .build()
            }

            override fun connection(): Connection? = null
            override fun call(): Call = throw UnsupportedOperationException()
            override fun connectTimeoutMillis(): Int = 1000
            override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
            override fun readTimeoutMillis(): Int = 1000
            override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
            override fun writeTimeoutMillis(): Int = 1000
            override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        }

        val interceptor = EmergencyRetryInterceptor(maxRetries = 3, initialBackoffMs = 1L)
        val response = interceptor.intercept(fakeChain)

        assertEquals(200, response.code)
        assertEquals(2, callCount)
    }
}
