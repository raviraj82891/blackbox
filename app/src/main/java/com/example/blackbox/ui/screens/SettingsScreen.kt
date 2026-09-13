package com.example.blackbox.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blackbox.ui.designsystem.*
import com.example.blackbox.ui.theme.*

@Composable
fun SettingsScreen(
    isAutoDetectionEnabled: Boolean = true,
    onAutoDetectionChanged: (Boolean) -> Unit = {},
    impactThreshold: Double,
    gyroThreshold: Double,
    isCalibrating: Boolean,
    calibrationProgress: Float,
    calibrationResultMessage: String? = null,
    onStartCalibrationClicked: () -> Unit,
    onImpactThresholdChanged: (Double) -> Unit,
    onGyroThresholdChanged: (Double) -> Unit,
    onClearBufferClicked: () -> Unit = {},
    onDeleteIncidentsClicked: () -> Unit = {},
    onFactoryResetClicked: () -> Unit = {},
    onSimulateCrashClicked: () -> Unit,
    onTestSosClicked: () -> Unit,
    onNavigateToContacts: () -> Unit = {},
    onNavigateToMedicalId: () -> Unit = {}
) {
    var showAdvancedMeasurement by remember { mutableStateOf(false) }
    var showDeveloperSection by remember { mutableStateOf(false) }
    var showClearBufferDialog by remember { mutableStateOf(false) }
    var showDeleteIncidentsDialog by remember { mutableStateOf(false) }
    var showWipeConfirmation by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = TraceCanvas,
        topBar = {
            TraceTopBar(
                title = "SETTINGS",
                subtitle = "DETECTION SENSITIVITY & DATA CONTROLS"
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 1. PROTECTION & DETECTION
            TraceSectionHeader(title = "PROTECTION & DETECTION")

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TraceHairline, RectangleShape),
                color = TraceSurface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AUTOMATIC DETECTION",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TracePrimary,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Detect possible crashes and severe falls automatically. Manual SOS remains always available.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TraceMuted
                            )
                        }
                        Switch(checked = isAutoDetectionEnabled, onCheckedChange = onAutoDetectionChanged)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    TraceDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "CRASH DETECTION SENSITIVITY",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TracePrimary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Adjust how easily high-g motion spikes trigger emergency alerts",
                        style = MaterialTheme.typography.bodySmall,
                        color = TraceMuted
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Slider(
                        value = impactThreshold.toFloat(),
                        onValueChange = { onImpactThresholdChanged(it.toDouble()) },
                        valueRange = 15f..45f,
                        steps = 30,
                        colors = SliderDefaults.colors(
                            thumbColor = TraceMintSuccess,
                            activeTrackColor = TraceMintSuccess,
                            inactiveTrackColor = TraceHairline
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("MORE SENSITIVE", style = MaterialTheme.typography.labelSmall, color = TraceMintSuccess, fontWeight = FontWeight.Bold)
                        Text("BALANCED", style = MaterialTheme.typography.labelSmall, color = TraceMuted)
                        Text("FEWER FALSE ALARMS", style = MaterialTheme.typography.labelSmall, color = TraceMintSuccess, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (showAdvancedMeasurement) "HIDE RAW VALUES" else "SHOW ADVANCED MEASUREMENT VALUES →",
                        style = MaterialTheme.typography.labelSmall,
                        color = TraceMintSuccess,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { showAdvancedMeasurement = !showAdvancedMeasurement }
                            .padding(vertical = 4.dp)
                    )

                    if (showAdvancedMeasurement) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, TraceHairline, RectangleShape),
                            color = TraceSoftSurface
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("RAW PHYSICS ENGINE THRESHOLDS:", style = MaterialTheme.typography.labelSmall, color = TracePrimary, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("• Impact Force Trigger: %.1f m/s² (approx %.1f G)".format(impactThreshold, impactThreshold / 9.81), style = MaterialTheme.typography.bodySmall, color = TraceMuted)
                                Text("• Angular Rotation Delta: %.1f rad/s".format(gyroThreshold), style = MaterialTheme.typography.bodySmall, color = TraceMuted)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    TraceDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "PERSONAL MOTION CALIBRATION",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TracePrimary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Carry your phone normally for 10 seconds to measure real sensor data and set a custom baseline for your movement style.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TraceMuted
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (calibrationResultMessage != null) {
                        Text(
                            text = calibrationResultMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = TracePrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    }

                    if (isCalibrating) {
                        LinearProgressIndicator(
                            progress = { calibrationProgress },
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = TraceMintSuccess
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Measuring accelerometer baseline... ${(calibrationProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = TraceMintSuccess
                        )
                    } else {
                        TraceSecondaryButton(
                            text = "START 10S SENSOR CALIBRATION",
                            onClick = onStartCalibrationClicked
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 2. PRIVACY & DATA CONTROLS
            TraceSectionHeader(title = "PRIVACY & LOCAL DATA CONTROLS")

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TraceHairline, RectangleShape),
                color = TraceSurface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "LOCAL DATA STORAGE SPECIFICATION",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = TracePrimary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• 60-Minute Sensor Buffer: Temporarily recorded in RAM/encrypted Room DB. Older entries automatically purged.", style = MaterialTheme.typography.bodySmall, color = TraceMuted)
                    Text("• Incident Reports: Encrypted client-side with AES-256-GCM before upload.", style = MaterialTheme.typography.bodySmall, color = TraceMuted)
                    Text("• Emergency Contacts: Stored in the encrypted local Room database.", style = MaterialTheme.typography.bodySmall, color = TraceMuted)
                    Text("• Medical ID Profile: Stored using secure encrypted app storage (EncryptedSharedPreferences).", style = MaterialTheme.typography.bodySmall, color = TraceMuted)

                    Spacer(modifier = Modifier.height(16.dp))
                    TraceDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "GRANULAR DATA WIPE SCOPES",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = TracePrimary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TraceSecondaryButton(
                            text = "CLEAR BUFFER",
                            onClick = { showClearBufferDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                        TraceSecondaryButton(
                            text = "DELETE INCIDENTS",
                            onClick = { showDeleteIncidentsDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TracePrimaryButton(
                        text = "FACTORY RESET TRACE (PURGE ALL DATA)",
                        onClick = { showWipeConfirmation = true },
                        isCritical = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 3. ADVANCED DEVELOPER TESTING
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TraceHairline, RectangleShape),
                color = TraceSurface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDeveloperSection = !showDeveloperSection },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DEVELOPER & TESTING TOOLS",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = TracePrimary,
                            letterSpacing = 1.sp
                        )
                        Icon(
                            imageVector = if (showDeveloperSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = TraceMuted
                        )
                    }

                    AnimatedVisibility(visible = showDeveloperSection) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            TraceDivider()
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Notice: Simulated triggers generate synthetic test telemetry for demonstration and verification.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TraceMuted
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            TraceSecondaryButton(
                                text = "INJECT SIMULATED CRASH SEQUENCE",
                                onClick = onSimulateCrashClicked
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            TraceSecondaryButton(
                                text = "TEST EMERGENCY COUNTDOWN",
                                onClick = onTestSosClicked
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(64.dp))
        }
    }

    // Clear Buffer Dialog
    if (showClearBufferDialog) {
        AlertDialog(
            onDismissRequest = { showClearBufferDialog = false },
            shape = RectangleShape,
            containerColor = TraceSurface,
            title = { Text("CLEAR SENSOR BUFFER ONLY?", fontWeight = FontWeight.Bold, color = TracePrimary, letterSpacing = 1.sp) },
            text = { Text("This will erase only the rolling 60-minute sensor telemetry logs. Incident reports, emergency contacts, and Medical ID will be preserved.", style = MaterialTheme.typography.bodyMedium, color = TraceMuted) },
            confirmButton = {
                TracePrimaryButton(
                    text = "CLEAR SENSOR BUFFER",
                    onClick = {
                        showClearBufferDialog = false
                        onClearBufferClicked()
                    },
                    modifier = Modifier.width(180.dp)
                )
            },
            dismissButton = {
                TraceSecondaryButton(
                    text = "CANCEL",
                    onClick = { showClearBufferDialog = false },
                    modifier = Modifier.width(100.dp)
                )
            }
        )
    }

    // Delete Incidents Dialog
    if (showDeleteIncidentsDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteIncidentsDialog = false },
            shape = RectangleShape,
            containerColor = TraceSurface,
            title = { Text("DELETE INCIDENT HISTORY ONLY?", fontWeight = FontWeight.Bold, color = TracePrimary, letterSpacing = 1.sp) },
            text = { Text("This will erase only the saved incident evidence reports. Rolling sensor telemetry, emergency contacts, and Medical ID will be preserved.", style = MaterialTheme.typography.bodyMedium, color = TraceMuted) },
            confirmButton = {
                TracePrimaryButton(
                    text = "DELETE INCIDENTS",
                    onClick = {
                        showDeleteIncidentsDialog = false
                        onDeleteIncidentsClicked()
                    },
                    modifier = Modifier.width(160.dp)
                )
            },
            dismissButton = {
                TraceSecondaryButton(
                    text = "CANCEL",
                    onClick = { showDeleteIncidentsDialog = false },
                    modifier = Modifier.width(100.dp)
                )
            }
        )
    }

    // Factory Reset Confirmation Dialog
    if (showWipeConfirmation) {
        AlertDialog(
            onDismissRequest = { showWipeConfirmation = false },
            shape = RectangleShape,
            containerColor = TraceSurface,
            title = {
                Text(
                    text = "PERMANENTLY RESET TRACE?",
                    fontWeight = FontWeight.Bold,
                    color = TracePrimary,
                    letterSpacing = 1.sp
                )
            },
            text = {
                Column {
                    Text("This action cannot be undone. The following items will be permanently erased:", style = MaterialTheme.typography.bodyMedium, color = TraceMuted)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• 60-Minute Sensor Telemetry Buffer\n• Encrypted Incident Reports & Evidence Bundles\n• Saved Emergency Contacts\n• Encrypted Medical ID & Emergency QR Details\n• Motion Calibration Parameters & Sensitivity\n• Analytics Counters & App Preferences\n• Application Signing Key & Session Identity", style = MaterialTheme.typography.bodySmall, color = TraceMuted)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("TRACE protection will be stopped and the app reinitialized into first-run onboarding.", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TraceRedCritical)
                }
            },
            confirmButton = {
                TracePrimaryButton(
                    text = "FACTORY RESET TRACE",
                    onClick = {
                        showWipeConfirmation = false
                        onFactoryResetClicked()
                    },
                    isCritical = true,
                    modifier = Modifier.width(180.dp)
                )
            },
            dismissButton = {
                TraceSecondaryButton(
                    text = "CANCEL",
                    onClick = { showWipeConfirmation = false },
                    modifier = Modifier.width(100.dp)
                )
            }
        )
    }
}
