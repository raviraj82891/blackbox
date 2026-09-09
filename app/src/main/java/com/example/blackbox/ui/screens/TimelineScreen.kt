package com.example.blackbox.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blackbox.domain.fusion.SeverityLevel
import com.example.blackbox.domain.fusion.TimelineEntry
import com.example.blackbox.domain.fusion.TimelineSession
import com.example.blackbox.ui.theme.*

@Composable
fun TimelineScreen(
    sessions: List<TimelineSession>,
    isChainValid: Boolean
) {
    var selectedFilter by remember { mutableIntStateOf(0) } // 0: Sessions, 1: All Events

    Scaffold(containerColor = OffWhite) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Timeline", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

                Row {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = CharcoalText)
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.FilterList, contentDescription = null, tint = CharcoalText)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Segmented Filter Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == 0,
                    onClick = { selectedFilter = 0 },
                    label = { Text("Sessions", fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF0F172A),
                        selectedLabelColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedFilter == 1,
                    onClick = { selectedFilter = 1 },
                    label = { Text("All Events", fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF0F172A),
                        selectedLabelColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Security Proof Status Banner
            Surface(
                color = if (isChainValid) SoftGreenContainer else SoftCoralContainer,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isChainValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isChainValid) DarkTeal else WarmCoral,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isChainValid) "Your last hour is recorded safely & sealed with SHA-256" else "WARNING: Timeline Log Discrepancy Detected",
                        color = if (isChainValid) DarkTeal else WarmCoral,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (sessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Rolling 60m buffer collecting activity sessions...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedSlate
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(sessions) { session ->
                        TimelineSessionCard(session)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Sessions are automatically created based on your activity.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MutedSlate,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun TimelineSessionCard(session: TimelineSession) {
    var expanded by remember { mutableStateOf(false) }

    val (activityIcon, containerColor, iconColor) = when {
        session.activityState.contains("VEHICLE", ignoreCase = true) -> Triple(Icons.Default.DirectionsCar, SoftGreenContainer, Mint)
        session.activityState.contains("RUNNING", ignoreCase = true) -> Triple(Icons.AutoMirrored.Filled.DirectionsRun, SoftCoralContainer, WarmCoral)
        session.activityState.contains("WALKING", ignoreCase = true) -> Triple(Icons.AutoMirrored.Filled.DirectionsWalk, SoftBlueContainer, SoftTeal)
        else -> Triple(Icons.Default.Person, SoftOrangeContainer, Peach)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = containerColor,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(activityIcon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = session.activityState,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${session.formattedTimeRange} • ${session.durationMinutes} min",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedSlate
                        )
                    }
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MutedSlate
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    HorizontalDivider(color = SurfaceTint)
                    Spacer(modifier = Modifier.height(10.dp))
                    session.entries.forEach { entry ->
                        TimelineEntryCard(entry)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineEntryCard(entry: TimelineEntry) {
    var showTechDetails by remember { mutableStateOf(false) }

    val badgeColor = when (entry.severityLevel) {
        SeverityLevel.CRITICAL -> WarmCoral
        SeverityLevel.WARNING -> Peach
        SeverityLevel.INFO -> SoftTeal
    }

    Surface(
        color = OffWhite,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(badgeColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = entry.formattedTime,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalText
                    )
                }
                Text(
                    text = entry.severityLevel.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = badgeColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = entry.summaryTitle,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = entry.detailedDescription,
                style = MaterialTheme.typography.labelSmall,
                color = MutedSlate
            )

            Spacer(modifier = Modifier.height(4.dp))

            TextButton(
                onClick = { showTechDetails = !showTechDetails },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(if (showTechDetails) "Hide Seal" else "Cryptographic Seal", fontSize = 10.sp)
            }

            if (showTechDetails) {
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    Text("SHA-256 Hash: ${entry.entryHash}", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = MutedSlate)
                }
            }
        }
    }
}
