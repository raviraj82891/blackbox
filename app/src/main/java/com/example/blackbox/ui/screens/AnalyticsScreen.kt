package com.example.blackbox.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blackbox.ui.theme.*

@Composable
fun AnalyticsScreen(
    totalEventsCount: Int,
    cancelledCountdownsCount: Int,
    autoCrashCount: Int,
    manualSosCount: Int
) {
    var timeFilter by remember { mutableIntStateOf(0) } // 0: 1H, 1: 1D, 2: 7D, 3: 30D

    Scaffold(containerColor = OffWhite) { padding ->
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
                Text(text = "Safety & Sensor Analytics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Time Filter Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("1H", "1D", "7D", "30D").forEachIndexed { index, label ->
                    FilterChip(
                        selected = timeFilter == index,
                        onClick = { timeFilter = index },
                        label = { Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0F172A),
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stat Cards Row
            Row(modifier = Modifier.fillMaxWidth()) {
                AnalyticsStatCard(
                    title = "Total Events",
                    value = "$totalEventsCount",
                    subtitle = "in last 60 minutes",
                    valueColor = SoftTeal,
                    containerColor = SoftBlueContainer,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                AnalyticsStatCard(
                    title = "False Alarms",
                    value = "$cancelledCountdownsCount",
                    subtitle = "avoided dispatches",
                    valueColor = WarmCoral,
                    containerColor = SoftCoralContainer,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Trigger Breakdown Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Trigger Breakdown",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    StatBarRow("Auto-Crash / Fall", autoCrashCount.coerceAtLeast(1), WarmCoral)
                    StatBarRow("Manual SOS", manualSosCount.coerceAtLeast(1), Peach)
                    StatBarRow("Cancelled False Alarms", cancelledCountdownsCount.coerceAtLeast(3), Mint)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Motion Intensity Wave Card
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
                        Text("Motion Intensity", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Surface(shape = RoundedCornerShape(8.dp), color = SoftGreenContainer) {
                            Text("● Live", color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bar Chart Canvas
                    Box(modifier = Modifier.fillMaxWidth().height(90.dp)) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val barWidth = 8.dp.toPx()
                            val space = 6.dp.toPx()
                            val count = (size.width / (barWidth + space)).toInt()

                            for (i in 0 until count) {
                                val hRatio = (Math.sin(i * 0.4) * 0.4 + 0.5).toFloat()
                                val barHeight = size.height * hRatio
                                drawRect(
                                    color = Mint,
                                    topLeft = Offset(i * (barWidth + space), size.height - barHeight),
                                    size = Size(barWidth, barHeight)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsStatCard(
    title: String,
    value: String,
    subtitle: String,
    valueColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MutedSlate)
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = valueColor)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MutedSlate, fontSize = 11.sp)
        }
    }
}

@Composable
private fun StatBarRow(label: String, value: Int, color: Color) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = CharcoalText)
            Text("$value", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = color)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (value / 10f).coerceIn(0.1f, 1f) },
            color = color,
            trackColor = SurfaceTint,
            modifier = Modifier.fillMaxWidth().height(6.dp)
        )
    }
}
