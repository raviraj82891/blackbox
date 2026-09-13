package com.example.blackbox.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blackbox.data.db.IncidentReport
import com.example.blackbox.data.db.TriggerType
import com.example.blackbox.data.db.UploadStatus
import com.example.blackbox.domain.trigger.CountdownState
import com.example.blackbox.service.ProtectionState
import com.example.blackbox.ui.designsystem.*
import com.example.blackbox.ui.theme.*
import com.example.blackbox.util.PermissionValidator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    protectionState: ProtectionState = ProtectionState.ACTIVE,
    protectionError: String? = null,
    isServiceRunning: Boolean = false,
    isBatterySaverActive: Boolean = false,
    batteryLevel: Int = 100,
    bufferEventCount: Int = 0,
    savedContactCount: Int = 0,
    isChainValid: Boolean? = null,
    sparklinePoints: List<Float> = emptyList(),
    situationalStatus: String = "Stationary",
    countdownState: CountdownState = CountdownState.Idle,
    lastActivatedReport: IncidentReport? = null,
    suggestAdaptiveThreshold: Boolean = false,
    safetyTimerSeconds: Int? = null,
    onPauseResumeClicked: () -> Unit = {},
    onManualSosClicked: () -> Unit = {},
    onCancelCountdownClicked: () -> Unit = {},
    onClearLastActivatedReport: () -> Unit = {},
    onApplyAdaptiveThreshold: () -> Unit = {},
    onDismissAdaptivePrompt: () -> Unit = {},
    onStartSafetyTimer: (Int) -> Unit = {},
    onCancelSafetyTimer: () -> Unit = {},
    onNavigateToContacts: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToIncidents: () -> Unit = {}
) {
    val context = LocalContext.current
    var showNotificationSheet by remember { mutableStateOf(false) }

    val missingPermissions = remember(isServiceRunning) {
        PermissionValidator.getMissingRequiredPermissions(context)
    }
    val hasLocationPerm = remember(context) { PermissionValidator.hasLocationPermission(context) }
    val hasMicPerm = remember(context) { PermissionValidator.hasMicPermission(context) }
    val hasActivityPerm = remember(context) { PermissionValidator.hasActivityPermission(context) }

    val statusText = when {
        protectionState == ProtectionState.ACTIVE && savedContactCount > 0 && missingPermissions.isEmpty() && isChainValid != false -> "PROTECTION ACTIVE"
        protectionState == ProtectionState.PERMISSION_LIMITED || missingPermissions.isNotEmpty() || savedContactCount == 0 -> "PROTECTION LIMITED"
        protectionState == ProtectionState.PAUSED -> "PROTECTION PAUSED"
        protectionState == ProtectionState.ERROR -> "PROTECTION ERROR"
        protectionState == ProtectionState.STARTING -> "STARTING PROTECTION..."
        else -> "PROTECTION PAUSED"
    }

    val detailsText = when {
        protectionState == ProtectionState.ERROR -> protectionError ?: "Service failed to start"
        protectionState == ProtectionState.PERMISSION_LIMITED || missingPermissions.isNotEmpty() -> "Missing required permissions: ${missingPermissions.joinToString(", ")}"
        savedContactCount == 0 -> "0 emergency contacts configured — Alert dispatches disabled"
        isChainValid == false -> "Cryptographic hash chain discrepancy detected"
        protectionState == ProtectionState.STARTING -> "Initializing sensor buffer..."
        protectionState == ProtectionState.PAUSED || protectionState == ProtectionState.STOPPED -> "Sensor recording engine is currently paused"
        else -> "Recording 60-min safety buffer • Ready for emergency dispatch"
    }

    val isEmergencyActive = countdownState is CountdownState.ActiveCountdown

    Scaffold(
        containerColor = TraceCanvas,
        topBar = {
            TraceTopBar(
                title = "TRACE",
                protectionState = protectionState,
                actions = {
                    IconButton(
                        onClick = { showNotificationSheet = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Status Log",
                            tint = TracePrimary
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // 1. HERO PROTECTION STATUS
                TraceSectionHeader(title = "PROTECTION STATUS")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TraceStatusDot(state = protectionState)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TracePrimary,
                        letterSpacing = 2.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = detailsText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TraceMuted
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 2. TECHNICAL SPECIFICATION SHEET GRID
                TraceSectionHeader(title = "HARDWARE & BUFFER SPECIFICATION")

                val motionVal = if (hasActivityPerm) situationalStatus.takeWhile { it != '—' }.trim() else "LIMITED"
                TraceSpecRow(label = "MOTION", value = motionVal, isOk = hasActivityPerm)

                val locVal = if (hasLocationPerm) "READY (GPS)" else "OFF"
                TraceSpecRow(label = "LOCATION", value = locVal, isOk = hasLocationPerm)

                val audioVal = if (!hasMicPerm) "OFF" else if (isBatterySaverActive) "PAUSED" else "READY"
                TraceSpecRow(label = "AUDIO", value = audioVal, isOk = hasMicPerm && !isBatterySaverActive)

                TraceSpecRow(label = "BATTERY", value = "$batteryLevel%", isOk = batteryLevel >= 15)

                val contactVal = if (savedContactCount > 0) "$savedContactCount READY" else "0 CONFIGURED"
                TraceSpecRow(
                    label = "CONTACTS",
                    value = contactVal,
                    isOk = savedContactCount > 0,
                    valueColor = if (savedContactCount == 0) TraceAmberWarning else null,
                    modifier = Modifier.clickable { onNavigateToContacts() }
                )

                val chainVal = when {
                    bufferEventCount == 0 -> "NO EVENTS YET"
                    isChainValid == true -> "VERIFIED"
                    isChainValid == false -> "WARNING"
                    else -> "CHECKING"
                }
                TraceSpecRow(
                    label = "BUFFER INTEGRITY",
                    value = "$chainVal ($bufferEventCount EVENTS)",
                    isOk = isChainValid == true || bufferEventCount == 0
                )

                Spacer(modifier = Modifier.height(28.dp))

                // 3. PRIMARY EMERGENCY SOS ACTION
                TraceEmergencyAction(
                    title = if (isEmergencyActive) "EMERGENCY ALERT ACTIVE" else "SOS EMERGENCY ALERT",
                    subtitle = if (isEmergencyActive) "COUNTDOWN IN PROGRESS — TAP TO CANCEL" else "HOLD FOR 3 SECONDS OR TAP TO TRIGGER ALERT",
                    onClick = onManualSosClicked,
                    isEmergencyActive = isEmergencyActive
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 4. SECONDARY COMPACT ACTIONS (Fix wrapping with TraceCompactButton)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TraceCompactButton(
                        text = if (isServiceRunning) "PAUSE" else "RESUME",
                        onClick = onPauseResumeClicked,
                        modifier = Modifier.weight(1f)
                    )
                    TraceCompactButton(
                        text = "SENSITIVITY",
                        onClick = onNavigateToSettings,
                        modifier = Modifier.weight(1f)
                    )
                    TraceCompactButton(
                        text = "REPORTS",
                        onClick = onNavigateToIncidents,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // 5. SOLO WALK SAFETY CHECK-IN TIMER
                TraceSectionHeader(title = "SOLO WALK CHECK-IN TIMER")

                if (safetyTimerSeconds != null) {
                    val mins = safetyTimerSeconds / 60
                    val secs = safetyTimerSeconds % 60
                    Text(
                        text = "TIMER ACTIVE: %02d:%02d REMAINING".format(mins, secs),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TraceRedCritical,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TracePrimaryButton(
                        text = "I'M SAFE — CANCEL TIMER",
                        onClick = onCancelSafetyTimer,
                        isCritical = false
                    )
                } else {
                    Text(
                        text = "Set a safety timer when walking alone. TRACE dispatches emergency alerts if you don't check in before expiration.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TraceMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TraceCompactButton(
                            text = "15 MIN",
                            onClick = { onStartSafetyTimer(15) },
                            modifier = Modifier.weight(1f)
                        )
                        TraceCompactButton(
                            text = "30 MIN",
                            onClick = { onStartSafetyTimer(30) },
                            modifier = Modifier.weight(1f)
                        )
                        TraceCompactButton(
                            text = "60 MIN",
                            onClick = { onStartSafetyTimer(60) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // 6. ADAPTIVE SENSITIVITY PROMPT
                if (suggestAdaptiveThreshold) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, TraceAmberWarning, RectangleShape),
                        color = TraceSurface
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "REDUCE CRASH SENSITIVITY?",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TracePrimary,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "You've cancelled a few alerts during normal activity. Reduce sensitivity to prevent false alarms?",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TraceMuted
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TracePrimaryButton(
                                    text = "REDUCE SENSITIVITY",
                                    onClick = onApplyAdaptiveThreshold,
                                    modifier = Modifier.weight(1f)
                                )
                                TraceSecondaryButton(
                                    text = "DISMISS",
                                    onClick = onDismissAdaptivePrompt,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // 7. RECENT MOTION GRAPH
                TraceSectionHeader(title = "RECENT MOTION")
                Text(
                    text = situationalStatus.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = TraceMuted,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .border(1.dp, TraceHairline, RectangleShape)
                        .background(TraceSoftSurface)
                        .padding(8.dp)
                ) {
                    SparklineCanvas(points = sparklinePoints, modifier = Modifier.fillMaxSize())
                }

                // Generous bottom content padding so content is never hidden behind bottom navigation bar
                Spacer(modifier = Modifier.height(64.dp))
            }

            // Emergency Countdown Overlay
            AnimatedVisibility(
                visible = countdownState is CountdownState.ActiveCountdown,
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                if (countdownState is CountdownState.ActiveCountdown) {
                    BackHandler(enabled = true) { /* User must tap Cancel button */ }

                    RedesignedCountdownOverlay(
                        secondsRemaining = countdownState.secondsRemaining,
                        triggerType = countdownState.triggerType,
                        impactMagnitude = countdownState.impactMagnitude,
                        savedContactCount = savedContactCount,
                        isLocationOn = hasLocationPerm,
                        onCancelClicked = onCancelCountdownClicked
                    )
                }
            }

            // Post-Expiration Alert Sent / Queued Result Modal
            lastActivatedReport?.let { report ->
                AlertDialog(
                    onDismissRequest = onClearLastActivatedReport,
                    shape = RectangleShape,
                    containerColor = TraceSurface,
                    title = {
                        Text(
                            text = when (report.uploadStatus) {
                                UploadStatus.NOTIFIED, UploadStatus.NOTIFICATION_REQUESTED -> "EMERGENCY ALERT DISPATCHED"
                                UploadStatus.UPLOADED -> "INCIDENT UPLOADED"
                                UploadStatus.PENDING -> "ALERT QUEUED (OFFLINE)"
                                else -> "DISPATCH FAILED"
                            }.uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = TracePrimary,
                            letterSpacing = 1.5.sp
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = when (report.uploadStatus) {
                                    UploadStatus.NOTIFIED, UploadStatus.NOTIFICATION_REQUESTED ->
                                        "An encrypted incident bundle and location notification were successfully dispatched to your emergency contacts."
                                    UploadStatus.PENDING ->
                                        "Network is currently unavailable. The encrypted incident bundle is queued locally and will be automatically uploaded as soon as connection is restored."
                                    else ->
                                        "Unable to reach the backend server. The incident is saved securely on your device."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = TraceMuted
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("REPORT ID: ${report.id.take(8)}", style = MaterialTheme.typography.labelSmall, color = TraceMuted)
                        }
                    },
                    confirmButton = {
                        TracePrimaryButton(
                            text = "OK",
                            onClick = onClearLastActivatedReport,
                            modifier = Modifier.width(100.dp)
                        )
                    }
                )
            }
        }
    }

    // System Notifications Bottom Sheet
    if (showNotificationSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNotificationSheet = false },
            shape = RectangleShape,
            containerColor = TraceSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                TraceSectionHeader(title = "PROTECTION DIAGNOSTIC LOGS")
                Spacer(modifier = Modifier.height(8.dp))

                TraceSpecRow("Foreground Service", if (isServiceRunning) "ACTIVE (60-MIN BUFFER)" else "PAUSED", isOk = isServiceRunning)
                TraceSpecRow("Emergency Contacts", if (savedContactCount > 0) "$savedContactCount READY" else "0 CONFIGURED", isOk = savedContactCount > 0)
                TraceSpecRow("GPS Service", if (hasLocationPerm) "ACTIVE" else "DENIED", isOk = hasLocationPerm)
                TraceSpecRow("Audio Classifier", if (!hasMicPerm) "DENIED" else if (isBatterySaverActive) "PAUSED (POWER SAVER)" else "ACTIVE", isOk = hasMicPerm && !isBatterySaverActive)
                TraceSpecRow("Hash Chain", if (isChainValid == true) "VERIFIED" else if (isChainValid == false) "WARNING" else "CHECKING", isOk = isChainValid == true)

                Spacer(modifier = Modifier.height(20.dp))
                TracePrimaryButton(
                    text = "CLOSE LOGS",
                    onClick = { showNotificationSheet = false }
                )
            }
        }
    }
}

// Redesigned Countdown Overlay for Stress Conditions
@Composable
private fun RedesignedCountdownOverlay(
    secondsRemaining: Int,
    triggerType: TriggerType,
    impactMagnitude: Double,
    savedContactCount: Int,
    isLocationOn: Boolean,
    onCancelClicked: () -> Unit
) {
    val isManualSos = triggerType == TriggerType.MANUAL_SOS

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(2.dp, TraceRedCritical, RectangleShape)
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = "Emergency countdown active. $secondsRemaining seconds remaining. ${if (isManualSos) "Manual SOS started." else "Possible incident detected."} Tap button below to cancel alert."
            },
        shape = RectangleShape,
        color = TraceCanvas
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isManualSos) "SOS STARTED" else "POSSIBLE INCIDENT DETECTED",
                color = TraceRedCritical,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isManualSos)
                    "You deliberately initiated a manual emergency alert."
                else
                    "High-impact motion was detected on your device.",
                color = TraceMuted,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "$secondsRemaining",
                color = TracePrimary,
                fontWeight = FontWeight.Black,
                fontSize = 72.sp
            )

            Text(
                text = "Emergency contacts will be alerted in $secondsRemaining seconds.",
                color = TracePrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (!isManualSos) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Text("IMPACT: %.1f m/s²".format(impactMagnitude), color = TraceMuted, style = MaterialTheme.typography.labelSmall)
                    Text("GPS: ${if (isLocationOn) "ACTIVE" else "OFF"}", color = TraceMuted, style = MaterialTheme.typography.labelSmall)
                    Text("CONTACTS: $savedContactCount", color = TraceMuted, style = MaterialTheme.typography.labelSmall)
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            TracePrimaryButton(
                text = if (isManualSos) "CANCEL SOS ALERT" else "I'M SAFE — CANCEL ALERT",
                onClick = onCancelClicked,
                isCritical = true
            )
        }
    }
}

@Composable
private fun SparklineCanvas(points: List<Float>, modifier: Modifier = Modifier) {
    if (points.size < 2) return

    val maxVal = (points.maxOrNull() ?: 15f).coerceAtLeast(20f)
    val minVal = (points.minOrNull() ?: 5f).coerceAtMost(5f)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val path = Path()

        val stepX = width / (points.size - 1)

        points.forEachIndexed { index, point ->
            val x = index * stepX
            val normalizedY = (point - minVal) / (maxVal - minVal)
            val y = height - (normalizedY * height)

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = TraceMintSuccess,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
