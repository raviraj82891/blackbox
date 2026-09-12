package com.example.blackbox.data.api

import com.example.blackbox.data.crypto.KeyManagementService
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Authentication Interceptor — Attaches bearer tokens to outgoing HTTP requests
 * and handles token expiration / 401 Unauthorized / 403 Forbidden responses centrally.
 */
class AuthInterceptor(
    private val keyManagementService: KeyManagementService
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = keyManagementService.getAuthToken()

        val requestBuilder = originalRequest.newBuilder()
        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        val response = chain.proceed(requestBuilder.build())

        // Handle 401/403 session expiration centrally
        if (response.code == 401 || response.code == 403) {
            keyManagementService.clearAuthSession()
        }

        return response
    }
}
