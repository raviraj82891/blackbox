package com.example.blackbox.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.example.blackbox.ui.designsystem.*
import com.example.blackbox.ui.theme.*

@Composable
fun DebugScreen(
    onSimulateCrashClicked: () -> Unit,
    onManualSosClicked: () -> Unit
) {
    Scaffold(
        containerColor = TraceCanvas,
        topBar = {
            TraceTopBar(
                title = "DEBUG TOOLS",
                subtitle = "DEVELOPER DEMO & SIMULATION CONTROL PANEL"
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

            TraceSectionHeader(title = "SIMULATED INCIDENT TRIGGER")

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TraceHairline, RectangleShape),
                color = TraceSurface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Notice: Simulated triggers generate synthetic test telemetry for demonstration and verification purposes only.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TraceMuted
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    TracePrimaryButton(
                        text = "INJECT SIMULATED CRASH SEQUENCE",
                        onClick = onSimulateCrashClicked
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TracePrimaryButton(
                        text = "TRIGGER MANUAL SOS COUNTDOWN",
                        onClick = onManualSosClicked,
                        isCritical = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
