package com.example.blackbox.ui.screens

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
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blackbox.domain.trigger.CountdownState

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
    onPauseResumeClicked: () -> Unit,
    onWipeDataClicked: () -> Unit,
    onManualSosClicked: () -> Unit,
    onCancelCountdownClicked: () -> Unit,
    onApplyAdaptiveThreshold: () -> Unit,
    onDismissAdaptivePrompt: () -> Unit
) {
    var showWipeConfirmation by remember { mutableStateOf(false) }

    // Pulse animation for hash-chain status
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Scaffold { padding ->
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

                // Contact Requirement Warning Banner
                if (savedContactCount == 0) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "PARTIAL PROTECTION: Add an Emergency Contact in the Contacts tab to enable incident alerts.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Battery Saver Warning Banner (Feature 2.5)
                if (isBatterySaverActive) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF78350F)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.BatteryAlert, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "POWER SAVER ACTIVE (<15% Battery): Audio classification paused and location relaxed to preserve emergency power.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Adaptive Threshold Suggestion Card (Feature 2.3)
                if (suggestAdaptiveThreshold) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Adaptive Sensitivity Prompt",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Frequent cancellations detected. Would you like to automatically raise the impact threshold to reduce false alerts during normal activity?",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row {
                                Button(
                                    onClick = onApplyAdaptiveThreshold,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Raise Threshold")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(onClick = onDismissAdaptivePrompt) {
                                    Text("Dismiss")
                                }
                            }
                        }
                    }
                }

                // Main Status Header Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isServiceRunning && savedContactCount > 0) Color(0xFF0F172A) else Color(0xFF334155)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .alpha(if (isServiceRunning) pulseAlpha else 1f)
                                    .background(
                                        color = if (isServiceRunning && savedContactCount > 0) Color(0xFF22C55E) else Color(0xFFF59E0B),
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isServiceRunning && savedContactCount > 0) "FULL BLACK BOX PROTECTION ACTIVE" else if (isServiceRunning) "PARTIAL PROTECTION ACTIVE" else "PAUSED",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = situationalStatus,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Feature 2.1: Real-time Accelerometer Sparkline Chart
                Text(
                    text = "Live Motion Vector Sparkline (2m Window)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        SparklineCanvas(points = sparklinePoints, modifier = Modifier.fillMaxSize())
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Buffer Metrics Section
                Text(
                    text = "Buffer Health & Security",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    MetricCard(
                        title = "Buffered Events",
                        value = "$bufferEventCount",
                        subtitle = "60m Rolling Window",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    MetricCard(
                        title = "Chain Integrity",
                        value = if (isChainValid) "VERIFIED" else "TAMPERED",
                        subtitle = if (isChainValid) "SHA-256 Intact" else "Discrepancy",
                        isSuccess = isChainValid,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Quick Action Controls
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onPauseResumeClicked,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isServiceRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isServiceRunning) "Pause" else "Resume")
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    OutlinedButton(
                        onClick = { showWipeConfirmation = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("One-Tap Wipe")
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Manual SOS Button (Quick 3s Activation)
                Button(
                    onClick = onManualSosClicked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "MANUAL EMERGENCY SOS",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Countdown Overlay
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
            title = { Text("Purge All Black Box Data?") },
            text = { Text("This will permanently erase all 60-minute sensor buffer logs and saved incident reports from this device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showWipeConfirmation = false
                        onWipeDataClicked()
                    }
                ) {
                    Text("Wipe Everything", color = MaterialTheme.colorScheme.error)
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
            color = Color(0xFF0284C7),
            style = Stroke(width = 3.dp.toPx())
        )
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    isSuccess: Boolean = true,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isSuccess) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
            )
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF7F1D1D),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "POSSIBLE $triggerType DETECTED!",
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
                text = "Freezing buffer and alerting contacts in $secondsRemaining s...",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onCancelClicked,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("I'M OK — CANCEL SOS", color = Color(0xFF7F1D1D), fontWeight = FontWeight.Bold)
            }
        }
    }
}
