package com.example.blackbox.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blackbox.domain.fusion.SeverityLevel
import com.example.blackbox.domain.fusion.TimelineEntry
import com.example.blackbox.domain.fusion.TimelineSession
import com.example.blackbox.ui.designsystem.*
import com.example.blackbox.ui.theme.*

@Composable
fun TimelineScreen(
    sessions: List<TimelineSession>,
    isChainValid: Boolean?
) {
    Scaffold(
        containerColor = TraceCanvas,
        topBar = {
            TraceTopBar(
                title = "TIMELINE",
                subtitle = "ROLLING 60-MINUTE BUFFER"
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Buffer Integrity Status Row
            val hasEvents = sessions.isNotEmpty()
            val chainStatus = when {
                !hasEvents -> "NO EVENTS YET"
                isChainValid == true -> "VERIFIED"
                isChainValid == false -> "WARNING"
                else -> "CHECKING"
            }
            val chainColor = when {
                !hasEvents -> TraceMuted
                isChainValid == true -> TraceMintSuccess
                isChainValid == false -> TraceRedCritical
                else -> TraceAmberWarning
            }

            Surface(
                color = TraceSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TraceHairline, RectangleShape)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "BUFFER INTEGRITY",
                            style = MaterialTheme.typography.labelSmall,
                            color = TraceMuted,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when {
                                !hasEvents -> "No telemetry events recorded in 60-min window"
                                isChainValid == true -> "SHA-256 Chain Anchored & Verified"
                                isChainValid == false -> "Integrity Warning — Chain Discrepancy"
                                else -> "Checking Cryptographic Integrity..."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TracePrimary
                        )
                    }
                    TraceBadge(text = chainStatus, color = chainColor)
                }
            }

            TraceDivider()

            if (sessions.isEmpty()) {
                TraceEmptyState(
                    title = "NO RECENT EVENTS",
                    description = "TRACE is continuously recording sensor telemetry over a rolling 60-minute window. Recorded motion, location, and acoustic events will appear here."
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 64.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(sessions) { session ->
                        TimelineSessionItem(session)
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineSessionItem(session: TimelineSession) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, TraceHairline, RectangleShape)
            .clickable { expanded = !expanded }
            .semantics {
                contentDescription = "${session.activityState} session from ${session.formattedTimeRange}. Peak severity: ${session.worstSeverity.name}."
            },
        color = TraceSurface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Session Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "${session.activityState} SESSION".uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TracePrimary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${session.formattedTimeRange} (${session.durationMinutes} MIN)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TraceMuted
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TraceSeverityBadge(severity = session.worstSeverity)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = TraceMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (session.maxSpeedKmh > 0.0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "MAX SPEED: %.1f KM/H".format(session.maxSpeedKmh),
                    style = MaterialTheme.typography.labelSmall,
                    color = TraceMuted
                )
            }

            // Expanded Chronological Event Nodes
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    TraceDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    session.entries.forEachIndexed { index, entry ->
                        TimelineNodeRow(
                            entry = entry,
                            isLast = index == session.entries.size - 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineNodeRow(
    entry: TimelineEntry,
    isLast: Boolean
) {
    var showTechDetails by remember { mutableStateOf(false) }

    val nodeColor = when (entry.severityLevel) {
        SeverityLevel.CRITICAL -> TraceRedCritical
        SeverityLevel.WARNING -> TraceAmberWarning
        SeverityLevel.INFO -> TraceMintSuccess
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Event at ${entry.formattedTime}: ${entry.summaryTitle}. ${entry.detailedDescription}"
            }
    ) {
        // Vertical Line & Node Bullet
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(8.dp)
                    .background(nodeColor, CircleShape)
            )

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .weight(1f)
                        .padding(vertical = 2.dp)
                        .background(TraceHairline)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Node Content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = entry.formattedTime,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = TracePrimary
                )
                TraceSeverityBadge(severity = entry.severityLevel)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = entry.summaryTitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = TracePrimary
            )

            Text(
                text = entry.detailedDescription,
                style = MaterialTheme.typography.bodySmall,
                color = TraceMuted
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (showTechDetails) "HIDE EVIDENCE DETAILS" else "VIEW EVIDENCE DETAILS →",
                style = MaterialTheme.typography.labelSmall,
                color = TraceMintSuccess,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { showTechDetails = !showTechDetails }
                    .padding(vertical = 4.dp)
            )

            if (showTechDetails) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, TraceHairline, RectangleShape),
                    color = TraceSoftSurface
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("EVENT TYPE: ${entry.eventType.name}", style = MaterialTheme.typography.labelSmall, color = TracePrimary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("SHA-256 ENTRY HASH:\n${entry.entryHash}", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TraceMuted)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("RAW PAYLOAD:\n${entry.rawPayload}", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TraceMuted)
                    }
                }
            }
        }
    }
}
