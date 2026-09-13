package com.example.blackbox.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blackbox.ui.AnalyticsState
import com.example.blackbox.ui.designsystem.*
import com.example.blackbox.ui.theme.*

@Composable
fun AnalyticsScreen(
    state: AnalyticsState
) {
    Scaffold(
        containerColor = TraceCanvas,
        topBar = {
            TraceTopBar(
                title = "ANALYTICS",
                subtitle = "ON-DEVICE TELEMETRY & TRIGGER STATISTICS"
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

            // Time Scope Status Row
            Surface(
                color = TraceSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TraceHairline, RectangleShape)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SCOPE: ${state.timeRangeLabel.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TracePrimary,
                        letterSpacing = 1.5.sp
                    )
                    TraceBadge(text = "ON-DEVICE", color = TraceBlueInfo)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!state.hasData) {
                TraceEmptyState(
                    title = "NO TELEMETRY RECORDED",
                    description = "As TRACE records rolling sensor buffer events and processes emergency triggers, on-device telemetry statistics will appear here."
                )
            } else {
                // 1. TELEMETRY INGEST SUMMARY
                TraceSectionHeader(title = "TELEMETRY INGEST SUMMARY")

                TraceSpecRow(
                    label = "TOTAL BUFFER EVENTS",
                    value = if (state.totalSensorEvents > 0) "${state.totalSensorEvents}" else "0"
                )
                TraceSpecRow(
                    label = "CANCELLED COUNTDOWNS",
                    value = if (state.cancelledCountdownsCount > 0) "${state.cancelledCountdownsCount}" else "0"
                )

                Spacer(modifier = Modifier.height(28.dp))

                // 2. TRIGGER HISTORY SPECIFICATION
                TraceSectionHeader(title = "INCIDENT TRIGGER HISTORY")

                TraceSpecRow(label = "AUTO-CRASH DETECTIONS", value = if (state.autoCrashCount > 0) "${state.autoCrashCount}" else "0")
                TraceSpecRow(label = "AUTO-FALL DETECTIONS", value = if (state.autoFallCount > 0) "${state.autoFallCount}" else "0")
                TraceSpecRow(label = "MANUAL SOS ACTIVATIONS", value = if (state.manualSosCount > 0) "${state.manualSosCount}" else "0")
                TraceSpecRow(label = "SAFETY CHECK-IN TIMERS", value = if (state.safetyCheckInCount > 0) "${state.safetyCheckInCount}" else "0")

                Spacer(modifier = Modifier.height(28.dp))

                // 3. CLOUD BACKUP SPECIFICATION
                TraceSectionHeader(title = "CLOUD BACKUP SPECIFICATION")

                TraceSpecRow(label = "SUCCESSFUL DISPATCHES", value = if (state.successfulUploadsCount > 0) "${state.successfulUploadsCount}" else "0")
                TraceSpecRow(
                    label = "FAILED DISPATCHES",
                    value = if (state.failedUploadsCount > 0) "${state.failedUploadsCount}" else "0",
                    isOk = state.failedUploadsCount == 0
                )

                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }
}
