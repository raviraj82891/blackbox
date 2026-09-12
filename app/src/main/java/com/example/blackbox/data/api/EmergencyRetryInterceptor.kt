package com.example.blackbox.data.api

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Emergency Retry Interceptor — Executes up to 3 automatic retries with exponential backoff
 * for emergency data dispatches (e.g. incident bundle uploads) on 5xx server errors or network IOExceptions.
 */
class EmergencyRetryInterceptor(
    private val maxRetries: Int = 3,
    private val initialBackoffMs: Long = 1000L
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var attempt = 0
        var response: Response? = null
        var lastException: IOException? = null

        while (attempt < maxRetries) {
            attempt++
            try {
                response = chain.proceed(request)
                if (response.isSuccessful || response.code < 500) {
                    return response
                }
            } catch (e: IOException) {
                lastException = e
            }

            if (attempt < maxRetries) {
                val backoffDelay = initialBackoffMs * (1 shl (attempt - 1))
                try {
                    Thread.sleep(backoffDelay)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }
        }

        if (response != null) {
            return response
        }
        throw lastException ?: IOException("Emergency request failed after $maxRetries retries")
    }
}
