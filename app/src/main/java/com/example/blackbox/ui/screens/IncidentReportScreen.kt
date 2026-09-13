package com.example.blackbox.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.blackbox.data.db.IncidentReport
import com.example.blackbox.data.db.UploadStatus
import com.example.blackbox.ui.designsystem.*
import com.example.blackbox.ui.theme.*
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun IncidentReportScreen(
    reports: List<IncidentReport>,
    uploadingReportIds: Set<String> = emptySet(),
    onUploadClicked: (IncidentReport) -> Unit,
    onExportPdfClicked: (IncidentReport) -> File,
    onVerifyIntegrityClicked: (IncidentReport) -> Boolean
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    var selectedPlaybackReport by remember { mutableStateOf<IncidentReport?>(null) }
    var verifyingReport by remember { mutableStateOf<IncidentReport?>(null) }
    var verificationResult by remember { mutableStateOf<Boolean?>(null) }
    var showHowReportsWorkDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = TraceCanvas,
        topBar = {
            TraceTopBar(
                title = "INCIDENTS",
                subtitle = "ENCRYPTED EVIDENCE BUNDLES"
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (reports.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, start = 20.dp, end = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "NO INCIDENTS RECORDED",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TracePrimary,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Incident reports are created automatically when an emergency alert completes. Evidence is encrypted on-device with AES-256-GCM.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TraceMuted,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "HOW INCIDENT REPORTS WORK →",
                        style = MaterialTheme.typography.labelSmall,
                        color = TraceMintSuccess,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier
                            .clickable { showHowReportsWorkDialog = true }
                            .padding(vertical = 8.dp, horizontal = 12.dp)
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 64.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(reports) { report ->
                        val isUploading = uploadingReportIds.contains(report.id)

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, TraceHairline, RectangleShape),
                            color = TraceSurface
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // Header: Trigger Type & Severity Score Badge
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${report.triggerType.name} INCIDENT".uppercase(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TracePrimary,
                                        letterSpacing = 1.sp
                                    )
                                    TraceBadge(
                                        text = "SEVERITY ${report.severityScore}/100",
                                        color = if (report.severityScore > 75) TraceRedCritical else if (report.severityScore > 40) TraceAmberWarning else TraceMintSuccess
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                TraceSpecRow(
                                    label = "STATUS",
                                    value = when {
                                        isUploading -> "UPLOADING..."
                                        report.uploadStatus == UploadStatus.NOTIFIED -> "CONTACTS NOTIFIED"
                                        report.uploadStatus == UploadStatus.NOTIFICATION_REQUESTED -> "NOTIFICATION DISPATCHED"
                                        report.uploadStatus == UploadStatus.UPLOADED -> "UPLOADED TO CLOUD"
                                        report.uploadStatus == UploadStatus.ENCRYPTED -> "ENCRYPTED ON-DEVICE"
                                        report.uploadStatus == UploadStatus.PENDING -> "PENDING RETRY"
                                        else -> "UPLOAD FAILED"
                                    },
                                    isOk = report.uploadStatus != UploadStatus.FAILED
                                )

                                if (isUploading) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp), color = TraceMintSuccess)
                                }

                                TraceSpecRow(label = "REPORT ID", value = report.id.take(8))
                                TraceSpecRow(label = "DATE & TIME", value = dateFormat.format(Date(report.triggeredAt)))

                                if (report.lastUploadedAt != null) {
                                    TraceSpecRow(label = "LAST UPLOADED", value = dateFormat.format(Date(report.lastUploadedAt)))
                                }

                                if (report.lastUploadError != null && report.uploadStatus != UploadStatus.NOTIFIED) {
                                    Text(
                                        text = "ERROR: ${report.lastUploadError}".uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TraceRedCritical,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Action Buttons
                                TraceSecondaryButton(
                                    text = "VERIFY DIGITAL SIGNATURE & CHAIN",
                                    onClick = {
                                        verifyingReport = report
                                        verificationResult = onVerifyIntegrityClicked(report)
                                    }
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                TracePrimaryButton(
                                    text = "INCIDENT REPLAY",
                                    onClick = { selectedPlaybackReport = report }
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    TraceSecondaryButton(
                                        text = "EXPORT PDF",
                                        onClick = {
                                            val pdfFile = onExportPdfClicked(report)
                                            val uri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                pdfFile
                                            )
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "application/pdf"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share Incident PDF"))
                                        },
                                        modifier = Modifier.weight(1f)
                                    )

                                    TraceSecondaryButton(
                                        text = if (report.uploadStatus == UploadStatus.FAILED || report.uploadStatus == UploadStatus.PENDING) "RETRY BACKUP" else "CLOUD BACKUP",
                                        onClick = { onUploadClicked(report) },
                                        enabled = !isUploading,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showHowReportsWorkDialog) {
        AlertDialog(
            onDismissRequest = { showHowReportsWorkDialog = false },
            shape = RectangleShape,
            containerColor = TraceSurface,
            title = { Text("HOW INCIDENT REPORTS WORK", fontWeight = FontWeight.Bold, color = TracePrimary, letterSpacing = 1.sp) },
            text = {
                Column {
                    Text("1. Continuous 60-Minute Telemetry Buffer captures high-frequency accelerometer, gyroscope, location, and acoustic data in encrypted RAM/local storage.", style = MaterialTheme.typography.bodyMedium, color = TraceMuted)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("2. Upon crash detection or manual SOS, a 30-second countdown gives you time to cancel if safe.", style = MaterialTheme.typography.bodyMedium, color = TraceMuted)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("3. If uncancelled, the rolling buffer is frozen, Merkle root hash computed, signed with Android Keystore RSA key, encrypted with AES-256-GCM, and dispatched to your contacts.", style = MaterialTheme.typography.bodyMedium, color = TraceMuted)
                }
            },
            confirmButton = {
                TracePrimaryButton(
                    text = "UNDERSTOOD",
                    onClick = { showHowReportsWorkDialog = false }
                )
            }
        )
    }

    // Integrity Result Dialog
    if (verifyingReport != null && verificationResult != null) {
        val isVerified = verificationResult == true
        AlertDialog(
            onDismissRequest = {
                verifyingReport = null
                verificationResult = null
            },
            shape = RectangleShape,
            containerColor = TraceSurface,
            title = {
                Text(
                    text = if (isVerified) "EVIDENCE INTEGRITY: VERIFIED" else "TAMPERING ALERT",
                    fontWeight = FontWeight.Bold,
                    color = if (isVerified) TraceMintSuccess else TraceRedCritical,
                    letterSpacing = 1.sp
                )
            },
            text = {
                Text(
                    text = if (isVerified)
                        "Recomputed SHA-256 hash chain matches completely and the Android Keystore RSA digital signature is authentic."
                    else
                        "Discrepancy detected! The hash chain or digital signature does not match stored records.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TraceMuted
                )
            },
            confirmButton = {
                TracePrimaryButton(
                    text = "OK",
                    onClick = {
                        verifyingReport = null
                        verificationResult = null
                    },
                    modifier = Modifier.width(100.dp)
                )
            }
        )
    }

    // Storytelling Incident Replay Dialog
    selectedPlaybackReport?.let { report ->
        IncidentReplayDialog(report = report, onDismiss = { selectedPlaybackReport = null })
    }
}

@Composable
private fun IncidentReplayDialog(report: IncidentReport, onDismiss: () -> Unit) {
    val timelineLines = remember(report) {
        val raw = report.timelineJson.lines().filter { it.isNotBlank() }
        if (raw.isNotEmpty()) raw else listOf("Emergency Incident Activated via ${report.triggerType}")
    }

    var currentStep by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }

    LaunchedEffect(isPlaying, currentStep) {
        if (isPlaying && currentStep < timelineLines.size - 1) {
            delay(1200)
            currentStep++
        } else if (currentStep >= timelineLines.size - 1) {
            isPlaying = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RectangleShape,
        containerColor = TraceSurface,
        title = {
            Text(
                text = "INCIDENT REPLAY",
                fontWeight = FontWeight.Bold,
                color = TracePrimary,
                letterSpacing = 1.5.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "CHRONOLOGICAL TELEMETRY STORYLINE:",
                    style = MaterialTheme.typography.labelSmall,
                    color = TraceMuted
                )
                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .border(1.dp, TraceHairline, RectangleShape),
                    color = TraceSoftSurface
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (timelineLines.isNotEmpty() && currentStep in timelineLines.indices) {
                            Text(
                                text = timelineLines[currentStep].uppercase(),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TracePrimary,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text("NO TELEMETRY AVAILABLE", color = TraceMuted)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (timelineLines.isNotEmpty()) {
                    Slider(
                        value = currentStep.toFloat(),
                        onValueChange = { currentStep = it.toInt().coerceIn(0, timelineLines.size - 1) },
                        valueRange = 0f..(timelineLines.size - 1).coerceAtLeast(1).toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = TraceMintSuccess,
                            activeTrackColor = TraceMintSuccess,
                            inactiveTrackColor = TraceHairline
                        )
                    )
                    Text("STEP ${currentStep + 1} OF ${timelineLines.size}", style = MaterialTheme.typography.labelSmall, color = TraceMuted, modifier = Modifier.align(Alignment.End))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    IconButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier
                            .size(48.dp)
                            .border(1.dp, TraceMintSuccess, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause Replay" else "Play Replay",
                            tint = TraceMintSuccess
                        )
                    }
                }
            }
        },
        confirmButton = {
            TracePrimaryButton(
                text = "CLOSE REPLAY",
                onClick = onDismiss
            )
        }
    )
}
