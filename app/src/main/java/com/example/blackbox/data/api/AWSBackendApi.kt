package com.example.blackbox.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Retrofit contract for AWS Serverless Backend (API Gateway -> Lambda -> S3 / DynamoDB / SNS).
 */
interface AWSBackendApi {

    @POST("auth/register")
    suspend fun register(@Body request: AuthRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body request: AuthRequest): Response<AuthResponse>

    @POST("contacts")
    suspend fun registerContact(@Body contact: EmergencyContactDto): Response<EmergencyContactDto>

    @POST("incidents")
    suspend fun uploadIncident(@Body request: IncidentUploadRequest): Response<IncidentUploadResponse>

    @GET("incidents/{id}")
    suspend fun getIncident(@Path("id") id: String): Response<IncidentDownloadResponse>

    @POST("incidents/{id}/notify")
    suspend fun notifyContacts(
        @Path("id") id: String,
        @Body request: NotificationRequest
    ): Response<NotificationResponse>
}
