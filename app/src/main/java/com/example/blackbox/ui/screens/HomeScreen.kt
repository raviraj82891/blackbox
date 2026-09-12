package com.example.blackbox.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blackbox.domain.trigger.CountdownState
import com.example.blackbox.ui.theme.*
import com.example.blackbox.util.PermissionValidator

@Composable
fun HomeScreen(
    isServiceRunning: Boolean,
    isBatterySaverActive: Boolean,
    bufferEventCount: Int,
    savedContactCount: Int,
    isChainValid: Boolean,
    sparklinePoints: List<Float>,
    situationalStatus: String,
    countdownState: CountdownState,
    suggestAdaptiveThreshold: Boolean,
    safetyTimerSeconds: Int?,
    onPauseResumeClicked: () -> Unit,
    onWipeDataClicked: () -> Unit,
    onManualSosClicked: () -> Unit,
    onCancelCountdownClicked: () -> Unit,
    onApplyAdaptiveThreshold: () -> Unit,
    onDismissAdaptivePrompt: () -> Unit,
    onStartSafetyTimer: (Int) -> Unit,
    onCancelSafetyTimer: () -> Unit,
    onNavigateToContacts: () -> Unit
) {
    val context = LocalContext.current
    var showWipeConfirmation by remember { mutableStateOf(false) }

    // Preflight check for missing permissions
    val missingPermissions = remember(isServiceRunning) {
        PermissionValidator.getMissingRequiredPermissions(context)
    }
    val isProtectionLimited = missingPermissions.isNotEmpty()

    // Subtle pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Scaffold(containerColor = OffWhite) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Row with TRACE Title & Alert Bell
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Mint,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "TRACE", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    }

                    Surface(
                        shape = CircleShape,
                        color = SoftCoralContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = WarmCoral, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // Add Emergency Contact Prompt (if 0 contacts saved)
                if (savedContactCount == 0) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = SoftCoralContainer)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    tint = WarmCoral,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Complete Your Setup",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CharcoalText
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "TRACE needs at least one emergency contact to notify if an accident occurs.",
                                fontSize = 13.sp,
                                color = MutedSlate
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = onNavigateToContacts,
                                colors = ButtonDefaults.buttonColors(containerColor = WarmCoral),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("Add Emergency Contact", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                // Battery Saver Warning Banner
                if (isBatterySaverActive) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = Peach
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.BatteryAlert, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Power Saver Active (<15% Battery): Audio classification paused to preserve emergency power.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Feature E: Safety Check-In Timer Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = SoftTeal)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Solo Walk Safety Check-In Timer", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        if (safetyTimerSeconds != null) {
                            val mins = safetyTimerSeconds / 60
                            val secs = safetyTimerSeconds % 60
                            Text(
                                text = "Check-In Timer Active: %02d:%02d remaining".format(mins, secs),
                                fontWeight = FontWeight.Bold,
                                color = WarmCoral,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onCancelSafetyTimer,
                                colors = ButtonDefaults.buttonColors(containerColor = Mint),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(44.dp)
                            ) {
                                Text("I'm Safe — Cancel Timer", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text("Set a timer when walking alone at night. If you don't check in before it expires, TRACE alerts your contacts.", style = MaterialTheme.typography.bodySmall, color = MutedSlate)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(onClick = { onStartSafetyTimer(15) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = SoftBlueContainer)) {
                                    Text("15 Min", color = DarkTeal, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Button(onClick = { onStartSafetyTimer(30) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = SoftBlueContainer)) {
                                    Text("30 Min", color = DarkTeal, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Button(onClick = { onStartSafetyTimer(60) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = SoftBlueContainer)) {
                                    Text("60 Min", color = DarkTeal, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Adaptive Threshold Suggestion Card
                if (suggestAdaptiveThreshold) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SoftBlueContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Adjust Crash Sensitivity?",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "You've cancelled a few alerts during normal motion. Would you like to raise the crash sensitivity threshold to avoid false alarms?",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedSlate
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row {
                                Button(
                                    onClick = onApplyAdaptiveThreshold,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SoftTeal)
                                ) {
                                    Text("Raise Sensitivity Threshold", color = Color.White)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(onClick = onDismissAdaptivePrompt) {
                                    Text("Dismiss")
                                }
                            }
                        }
                    }
                }

                // Main Protection Status Card (Preflight Check Result)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isProtectionLimited -> SoftCoralContainer
                            isServiceRunning && savedContactCount > 0 -> SoftGreenContainer
                            else -> SoftOrangeContainer
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = when {
                                isProtectionLimited -> WarmCoral
                                isServiceRunning && savedContactCount > 0 -> Mint
                                else -> Peach
                            },
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isProtectionLimited) Icons.Default.Warning else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = when {
                                isProtectionLimited -> "Protection Limited"
                                isServiceRunning && savedContactCount > 0 -> "Protection Active"
                                isServiceRunning -> "Protection Active (Contact Setup Needed)"
                                else -> "Protection Paused"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalText
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (isProtectionLimited)
                                "Missing required permissions: ${missingPermissions.joinToString(", ")}"
                            else
                                "Recording your safety buffer",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = if (isProtectionLimited) WarmCoral else MutedSlate
                        )

                        if (isProtectionLimited) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = WarmCoral),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Fix Permissions in App Settings", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3 Context Badges (Motion | Location | Battery/Contacts)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ContextBadge(
                        title = "Motion",
                        status = if (PermissionValidator.hasActivityPermission(context)) "Normal" else "Limited",
                        icon = Icons.Default.Speed,
                        containerColor = SoftGreenContainer,
                        iconColor = Mint,
                        modifier = Modifier.weight(1f)
                    )
                    ContextBadge(
                        title = "Location",
                        status = if (PermissionValidator.hasLocationPermission(context)) "On" else "Off",
                        icon = Icons.Default.MyLocation,
                        containerColor = if (PermissionValidator.hasLocationPermission(context)) SoftBlueContainer else SoftCoralContainer,
                        iconColor = if (PermissionValidator.hasLocationPermission(context)) SoftTeal else WarmCoral,
                        modifier = Modifier.weight(1f)
                    )
                    ContextBadge(
                        title = "Contacts",
                        status = "$savedContactCount saved",
                        icon = Icons.Default.Shield,
                        containerColor = SoftOrangeContainer,
                        iconColor = Peach,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Live Motion Sparkline Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Live Motion (Last 2 Minutes)",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = situationalStatus,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MutedSlate
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .alpha(pulseAlpha)
                                    .background(Mint, CircleShape)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                        ) {
                            SparklineCanvas(points = sparklinePoints, modifier = Modifier.fillMaxSize())
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Prominent Warm Coral SOS Action Button
                Button(
                    onClick = onManualSosClicked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WarmCoral)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "SOS",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "Hold for 3 seconds",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Quick Action Controls (Pause / Wipe)
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onPauseResumeClicked,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = if (isServiceRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isServiceRunning) "Pause Protection" else "Resume Protection")
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    OutlinedButton(
                        onClick = { showWipeConfirmation = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WarmCoral)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("One-Tap Wipe")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Emergency Countdown Overlay
            AnimatedVisibility(
                visible = countdownState is CountdownState.ActiveCountdown,
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                if (countdownState is CountdownState.ActiveCountdown) {
                    CountdownBanner(
                        secondsRemaining = countdownState.secondsRemaining,
                        triggerType = countdownState.triggerType.name,
                        onCancelClicked = onCancelCountdownClicked
                    )
                }
            }
        }
    }

    if (showWipeConfirmation) {
        AlertDialog(
            onDismissRequest = { showWipeConfirmation = false },
            title = { Text("Purge All Black Box Logs?") },
            text = { Text("This will permanently erase all 60-minute sensor buffer logs and saved incident reports from this device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showWipeConfirmation = false
                        onWipeDataClicked()
                    }
                ) {
                    Text("Wipe Everything", color = WarmCoral)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ContextBadge(
    title: String,
    status: String,
    icon: ImageVector,
    containerColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = containerColor,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, color = MutedSlate, fontSize = 11.sp)
            Text(status, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
            color = Mint,
            style = Stroke(width = 3.dp.toPx())
        )
    }
}

@Composable
private fun CountdownBanner(
    secondsRemaining: Int,
    triggerType: String,
    onCancelClicked: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        color = WarmCoral,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "EMERGENCY $triggerType ACTIVATED",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$secondsRemaining",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 48.sp
            )
            Text(
                text = "Notifying emergency contacts in $secondsRemaining s...",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onCancelClicked,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("I'M OK — CANCEL SOS", color = WarmCoral, fontWeight = FontWeight.Bold)
            }
        }
    }
}
