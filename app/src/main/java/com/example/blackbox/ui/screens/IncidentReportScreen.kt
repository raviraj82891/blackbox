package com.example.blackbox.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.blackbox.data.db.IncidentReport
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun IncidentReportScreen(
    reports: List<IncidentReport>,
    onUploadClicked: (IncidentReport) -> Unit,
    onExportPdfClicked: (IncidentReport) -> File,
    onVerifyIntegrityClicked: (IncidentReport) -> Boolean
) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    var selectedPlaybackReport by remember { mutableStateOf<IncidentReport?>(null) }
    var verifyingReport by remember { mutableStateOf<IncidentReport?>(null) }
    var verificationResult by remember { mutableStateOf<Boolean?>(null) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Your Saved Reports",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Encrypted incident reports generated during emergencies or manual SOS activations",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (reports.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No saved reports. Reports will appear here automatically if an emergency occurs or if you test manual SOS.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(reports) { report ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.errorContainer
                                    ) {
                                        Text(
                                            text = report.triggerType.name,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            fontSize = 12.sp
                                        )
                                    }

                                    // Feature 2.1: Incident Severity Score Badge (0-100)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = when {
                                            report.severityScore > 75 -> Color(0xFFDC2626)
                                            report.severityScore > 40 -> Color(0xFFD97706)
                                            else -> Color(0xFF2563EB)
                                        }
                                    ) {
                                        Text(
                                            text = "Severity Score: ${report.severityScore}/100",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Report ID: ${report.id.take(8)}... | ${dateFormat.format(Date(report.triggeredAt))}",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Feature 2.2: Verify Evidence Integrity Button
                                OutlinedButton(
                                    onClick = {
                                        verifyingReport = report
                                        verificationResult = onVerifyIntegrityClicked(report)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Verify Digital Signature & Hash Chain")
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Feature 2.3: Replay Black Box Timeline
                                Button(
                                    onClick = { selectedPlaybackReport = report },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Replay Incident Playback Mode")
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    OutlinedButton(
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
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Export PDF")
                                    }

                                    Button(
                                        onClick = { onUploadClicked(report) },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Cloud Backup")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Feature 2.2 Integrity Result Dialog
    if (verifyingReport != null && verificationResult != null) {
        val isVerified = verificationResult == true
        AlertDialog(
            onDismissRequest = {
                verifyingReport = null
                verificationResult = null
            },
            title = { Text("Evidence Integrity Status") },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isVerified) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isVerified) Color(0xFF16A34A) else Color(0xFFDC2626)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isVerified) "Evidence Integrity: VERIFIED" else "TAMPERING ALERT",
                            fontWeight = FontWeight.Bold,
                            color = if (isVerified) Color(0xFF16A34A) else Color(0xFFDC2626)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isVerified)
                            "Recomputed SHA-256 hash chain matches completely and the Android Keystore RSA digital signature is authentic."
                        else "Discrepancy detected! The hash chain or digital signature does not match stored records.",
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    verifyingReport = null
                    verificationResult = null
                }) {
                    Text("OK")
                }
            }
        )
    }

    // Feature 2.3 Black Box Timeline Playback Dialog
    selectedPlaybackReport?.let { report ->
        PlaybackDialog(report = report, onDismiss = { selectedPlaybackReport = null })
    }
}

@Composable
private fun PlaybackDialog(report: IncidentReport, onDismiss: () -> Unit) {
    val timelineLines = remember(report) { report.timelineJson.lines().filter { it.isNotBlank() } }
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
        title = { Text("Black Box Playback Mode", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Replaying chronological incident telemetry:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
                        if (timelineLines.isNotEmpty() && currentStep in timelineLines.indices) {
                            Text(
                                text = timelineLines[currentStep],
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            Text("No timeline telemetry available for playback.")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (timelineLines.isNotEmpty()) {
                    Slider(
                        value = currentStep.toFloat(),
                        onValueChange = { currentStep = it.toInt().coerceIn(0, timelineLines.size - 1) },
                        valueRange = 0f..(timelineLines.size - 1).coerceAtLeast(1).toFloat()
                    )
                    Text("Step ${currentStep + 1} of ${timelineLines.size}", fontSize = 11.sp, modifier = Modifier.align(Alignment.End))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    IconButton(onClick = { isPlaying = !isPlaying }) {
                        Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close Playback")
            }
        }
    )
}
