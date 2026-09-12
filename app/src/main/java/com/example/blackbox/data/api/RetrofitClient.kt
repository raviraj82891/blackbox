package com.example.blackbox.data.api

import com.example.blackbox.BuildConfig
import com.example.blackbox.data.crypto.KeyManagementService
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    fun createOkHttpClient(
        keyManagementService: KeyManagementService,
        isDebug: Boolean = BuildConfig.DEBUG
    ): OkHttpClient {
        val certPinnerBuilder = CertificatePinner.Builder()
        val certDomain = BuildConfig.CERT_PIN_DOMAIN
        val certHash = BuildConfig.CERT_PIN_HASH

        // Operational certificate pinning: enforces pinning only when valid domain and hash are configured
        if (certDomain.isNotBlank() && certHash.isNotBlank() && !certHash.contains("AAAAA")) {
            certPinnerBuilder.add(certDomain, certHash)
        }

        return OkHttpClient.Builder()
            .certificatePinner(certPinnerBuilder.build())
            .addInterceptor(AuthInterceptor(keyManagementService))
            .addInterceptor(EmergencyRetryInterceptor(maxRetries = 3, initialBackoffMs = 1000L))
            .addInterceptor(SanitizedLoggingInterceptor(isDebug = isDebug))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun createApiService(
        keyManagementService: KeyManagementService,
        baseUrl: String = BuildConfig.BASE_URL
    ): AWSBackendApi {
        val client = createOkHttpClient(keyManagementService)
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AWSBackendApi::class.java)
    }
}
