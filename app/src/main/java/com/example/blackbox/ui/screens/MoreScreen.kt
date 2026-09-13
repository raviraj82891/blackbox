package com.example.blackbox.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.blackbox.BuildConfig
import com.example.blackbox.ui.designsystem.*
import com.example.blackbox.ui.theme.*

@Composable
fun MoreScreen(
    savedContactCount: Int = 0,
    onNavigateToContacts: () -> Unit,
    onNavigateToMedicalId: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDebug: () -> Unit = {}
) {
    Scaffold(
        containerColor = TraceCanvas,
        topBar = {
            TraceTopBar(
                title = "MORE",
                subtitle = "SAFETY PROFILE, CONTACTS & PRIVACY SETTINGS"
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

            // 1. SAFETY SECTION
            TraceSectionHeader(title = "SAFETY")

            TraceSettingRow(
                title = "Emergency Contacts",
                subtitle = if (savedContactCount > 0) "$savedContactCount contact${if (savedContactCount > 1) "s" else ""} ready for emergency dispatch" else "0 contacts saved — Setup required",
                icon = Icons.Default.People,
                trailingText = if (savedContactCount > 0) "$savedContactCount READY" else "SETUP NEEDED",
                onClick = onNavigateToContacts
            )

            TraceSettingRow(
                title = "Medical Profile & Emergency QR",
                subtitle = "Encrypted medical details & opt-in first responder QR code",
                icon = Icons.Default.MedicalServices,
                trailingText = "ENCRYPTED",
                onClick = onNavigateToMedicalId
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 2. INSIGHTS SECTION
            TraceSectionHeader(title = "INSIGHTS")

            TraceSettingRow(
                title = "Local Safety Analytics",
                subtitle = "On-device telemetry stats & emergency trigger history",
                icon = Icons.Default.Analytics,
                trailingText = "TELEMETRY",
                onClick = onNavigateToAnalytics
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 3. PRIVACY & CONTROLS SECTION
            TraceSectionHeader(title = "PRIVACY & CONTROLS")

            TraceSettingRow(
                title = "Settings & Data Privacy",
                subtitle = "Sensitivity controls, motion calibration & granular data wipe",
                icon = Icons.Default.Settings,
                trailingText = "CONTROLS",
                onClick = onNavigateToSettings
            )

            // 4. ADVANCED SECTION (Debug builds only)
            if (BuildConfig.DEBUG) {
                Spacer(modifier = Modifier.height(24.dp))
                TraceSectionHeader(title = "ADVANCED")

                TraceSettingRow(
                    title = "Developer & Testing Tools",
                    subtitle = "Simulate crash sequences and test emergency countdowns",
                    icon = Icons.Default.BugReport,
                    trailingText = "DEBUG",
                    onClick = onNavigateToDebug
                )
            }

            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}
