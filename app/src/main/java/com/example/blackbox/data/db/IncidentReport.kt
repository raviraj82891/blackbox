package com.example.blackbox.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "incident_reports")
data class IncidentReport(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val triggeredAt: Long,
    val triggerType: TriggerType,
    val timelineJson: String,
    val chainRootHash: String,
    val uploadStatus: UploadStatus = UploadStatus.PENDING,
    val encryptedBundle: String? = null,
    val decryptionKey: String? = null
)
