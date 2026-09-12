package com.example.blackbox.data.api

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import java.io.IOException

/**
 * Sanitized Logging Interceptor for Debug Builds — Ensures safe HTTP logging
 * by strictly redacting authorization tokens, phone numbers, email addresses,
 * incident payloads, encryption keys, and medical data before printing.
 */
class SanitizedLoggingInterceptor(
    private val isDebug: Boolean = true,
    private val logger: (String) -> Unit = { println(it) }
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        if (!isDebug) {
            // Production Release: Do not log headers or bodies
            return chain.proceed(request)
        }

        // 1. Log Sanitized Request
        logRequest(request)

        val response = try {
            chain.proceed(request)
        } catch (e: IOException) {
            logger("--> HTTP FAILED: $e")
            throw e
        }

        // 2. Log Sanitized Response
        logResponse(response)

        return response
    }

    private fun logRequest(request: Request) {
        val url = request.url.toString()
        val method = request.method
        logger("--> $method $url")

        // Redact Authorization Header
        val authHeader = request.header("Authorization")
        if (authHeader != null) {
            logger("Authorization: [REDACTED_TOKEN]")
        }

        // Redact Request Body
        val body = request.body
        if (body != null) {
            val buffer = Buffer()
            body.writeTo(buffer)
            val rawBody = buffer.readUtf8()
            val sanitizedBody = sanitizeText(rawBody)
            logger("Request Body: $sanitizedBody")
        }
    }

    private fun logResponse(response: Response) {
        logger("<-- ${response.code} ${response.message} ${response.request.url}")
        val responseBody = response.body
        if (responseBody != null) {
            val source = responseBody.source()
            source.request(Long.MAX_VALUE)
            val buffer = source.buffer
            val rawBody = buffer.clone().readUtf8()
            val sanitizedBody = sanitizeText(rawBody)
            logger("Response Body: $sanitizedBody")
        }
    }

    fun sanitizeText(text: String): String {
        var sanitized = text

        // 1. Redact Bearer Tokens
        sanitized = sanitized.replace(Regex("(?i)Bearer\\s+[A-Za-z0-9\\-_\\.~\\+\\/]+=*"), "Bearer [REDACTED_TOKEN]")

        // 2. Redact Authorization Tokens in JSON
        sanitized = sanitized.replace(Regex("(?i)\"token\"\\s*:\\s*\".*?\""), "\"token\":\"[REDACTED_TOKEN]\"")

        // 3. Redact Phone Numbers (E.164 / International / Standard formats)
        sanitized = sanitized.replace(Regex("\\b\\+?[1-9]\\d{1,14}\\b"), "[REDACTED_PHONE]")
        sanitized = sanitized.replace(Regex("(?i)\"emergencyContactPhone\"\\s*:\\s*\".*?\""), "\"emergencyContactPhone\":\"[REDACTED_PHONE]\"")

        // 4. Redact Email Addresses
        sanitized = sanitized.replace(Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"), "[REDACTED_EMAIL]")

        // 5. Redact Incident Payloads & Encrypted Bundles
        sanitized = sanitized.replace(Regex("(?i)\"encryptedBundle\"\\s*:\\s*\".*?\""), "\"encryptedBundle\":\"[REDACTED_PAYLOAD]\"")
        sanitized = sanitized.replace(Regex("(?i)\"timelineJson\"\\s*:\\s*\".*?\""), "\"timelineJson\":\"[REDACTED_PAYLOAD]\"")

        // 6. Redact Encryption Keys & Passphrases
        sanitized = sanitized.replace(Regex("(?i)\"decryptionKey\"\\s*:\\s*\".*?\""), "\"decryptionKey\":\"[REDACTED_KEY]\"")
        sanitized = sanitized.replace(Regex("(?i)\"passphrase\"\\s*:\\s*\".*?\""), "\"passphrase\":\"[REDACTED_KEY]\"")

        // 7. Redact Medical Data
        sanitized = sanitized.replace(Regex("(?i)\"allergies\"\\s*:\\s*\".*?\""), "\"allergies\":\"[REDACTED_MEDICAL]\"")
        sanitized = sanitized.replace(Regex("(?i)\"bloodGroup\"\\s*:\\s*\".*?\""), "\"bloodGroup\":\"[REDACTED_MEDICAL]\"")
        sanitized = sanitized.replace(Regex("(?i)\"medicalNotes\"\\s*:\\s*\".*?\""), "\"medicalNotes\":\"[REDACTED_MEDICAL]\"")

        return sanitized
    }
}
