package com.example.blackbox.data.api

data class IncidentUploadRequest(
    val incidentId: String,
    val triggeredAt: Long,
    val triggerType: String,
    val encryptedBundle: String,
    val chainRootHash: String,
    val contactsToNotify: List<String>
)

data class IncidentUploadResponse(
    val incidentId: String,
    val status: String,
    val s3Key: String,
    val message: String
)

data class IncidentDownloadResponse(
    val incidentId: String,
    val triggeredAt: Long,
    val triggerType: String,
    val encryptedBundle: String,
    val chainRootHash: String
)

data class NotificationRequest(
    val incidentId: String,
    val message: String,
    val contacts: List<String>
)

data class NotificationResponse(
    val incidentId: String,
    val status: String,
    val notifiedCount: Int
)
