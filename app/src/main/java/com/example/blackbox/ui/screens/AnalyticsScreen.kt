package com.example.blackbox.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blackbox.ui.AnalyticsState
import com.example.blackbox.ui.theme.MutedSlate
import com.example.blackbox.ui.theme.SoftBlueContainer

@Composable
fun AnalyticsScreen(
    state: AnalyticsState
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Analytics,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Local Safety Analytics",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "100% on-device telemetry & emergency trigger statistics",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Time Range Scope Badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SoftBlueContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Scope: ${state.timeRangeLabel}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MutedSlate
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (!state.hasData) {
                // "No data yet" state
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(40.dp), tint = MutedSlate)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No data yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "As TRACE records sensor buffer events and handles emergency triggers, your local telemetry analytics will appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedSlate
                        )
                    }
                }
            } else {
                // Main Telemetry Summary Cards
                Row(modifier = Modifier.fillMaxWidth()) {
                    AnalyticsStatCard(
                        title = "Total Sensor Events",
                        value = if (state.totalSensorEvents > 0) "${state.totalSensorEvents}" else "No data yet",
                        subtitle = "60m Rolling Ingest",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    AnalyticsStatCard(
                        title = "Cancelled Countdown",
                        value = if (state.cancelledCountdownsCount > 0) "${state.cancelledCountdownsCount}" else "0",
                        subtitle = "False Positives Avoided",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Emergency Trigger Breakdown Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Incident Trigger History",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        StatRow("Auto-Crash Detections", if (state.autoCrashCount > 0) "${state.autoCrashCount}" else "No data yet")
                        StatRow("Auto-Fall Detections", if (state.autoFallCount > 0) "${state.autoFallCount}" else "No data yet")
                        StatRow("Manual SOS Activations", if (state.manualSosCount > 0) "${state.manualSosCount}" else "No data yet")
                        StatRow("Cancelled Countdowns", if (state.cancelledCountdownsCount > 0) "${state.cancelledCountdownsCount}" else "No data yet")
                        StatRow("Safety Check-In Activations", if (state.safetyCheckInCount > 0) "${state.safetyCheckInCount}" else "No data yet")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Cloud Upload & Storage Status Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Cloud, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cloud Dispatch & Storage", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        StatRow("Successful Cloud Dispatches", if (state.successfulUploadsCount > 0) "${state.successfulUploadsCount}" else "No uploads yet")
                        StatRow("Failed Cloud Dispatches", if (state.failedUploadsCount > 0) "${state.failedUploadsCount}" else "0")
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsStatCard(title: String, value: String, subtitle: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                value,
                style = if (value.contains("No data")) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(
            value,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodySmall,
            color = if (value.contains("No")) MutedSlate else MaterialTheme.colorScheme.primary
        )
    }
}
