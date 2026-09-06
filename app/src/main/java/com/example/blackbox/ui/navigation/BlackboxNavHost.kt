package com.example.blackbox.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.blackbox.ui.MainViewModel
import com.example.blackbox.ui.screens.*

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Onboarding : Screen("onboarding", "Onboarding", Icons.Default.Home)
    object Home : Screen("home", "Status", Icons.Default.Home)
    object Timeline : Screen("timeline", "Timeline", Icons.Default.List)
    object Incidents : Screen("incidents", "Incidents", Icons.Default.Report)
    object Contacts : Screen("contacts", "Contacts", Icons.Default.People)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object Debug : Screen("debug", "Debug", Icons.Default.BugReport)
}

@Composable
fun BlackboxNavHost(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    var isOnboardingCompleted by remember { mutableStateOf(false) }

    val navItems = listOf(
        Screen.Home,
        Screen.Timeline,
        Screen.Incidents,
        Screen.Contacts,
        Screen.Settings,
        Screen.Debug
    )

    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route ?: Screen.Home.route

    Scaffold(
        bottomBar = {
            if (isOnboardingCompleted && currentRoute != Screen.Onboarding.route) {
                NavigationBar {
                    navItems.forEach { screen ->
                        NavigationBarItem(
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(Screen.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        val isServiceRunning by viewModel.isServiceRunning.collectAsState()
        val bufferEventCount by viewModel.bufferEventCount.collectAsState()
        val isChainValid by viewModel.isChainIntegrityValid.collectAsState()
        val countdownState by viewModel.countdownState.collectAsState()
        val timelineEntries by viewModel.reconstructedTimeline.collectAsState()
        val incidentReports by viewModel.incidentReports.collectAsState()
        val contacts by viewModel.emergencyContacts.collectAsState()

        NavHost(
            navController = navController,
            startDestination = if (isOnboardingCompleted) Screen.Home.route else Screen.Onboarding.route,
            modifier = modifier.padding(padding)
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onGrantPermissionsAndStart = {
                        isOnboardingCompleted = true
                        viewModel.startProtectionService()
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    isServiceRunning = isServiceRunning,
                    bufferEventCount = bufferEventCount,
                    isChainValid = isChainValid,
                    countdownState = countdownState,
                    onPauseResumeClicked = {
                        if (isServiceRunning) viewModel.pauseProtectionService() else viewModel.resumeProtectionService()
                    },
                    onWipeDataClicked = { viewModel.wipeAllData() },
                    onManualSosClicked = { viewModel.triggerManualSos() },
                    onCancelCountdownClicked = { viewModel.cancelCountdown() }
                )
            }

            composable(Screen.Timeline.route) {
                TimelineScreen(
                    entries = timelineEntries,
                    isChainValid = isChainValid
                )
            }

            composable(Screen.Incidents.route) {
                IncidentReportScreen(
                    reports = incidentReports,
                    onUploadClicked = { report ->
                        // Trigger AWS Upload
                    },
                    onExportPdfClicked = { report ->
                        viewModel.exportPdfReport(report)
                    }
                )
            }

            composable(Screen.Contacts.route) {
                ContactsScreen(
                    contacts = contacts,
                    onAddContact = { viewModel.addEmergencyContact(it) },
                    onRemoveContact = { viewModel.removeEmergencyContact(it) }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    impactThreshold = viewModel.triggerDetector.impactThresholdMs2,
                    gyroThreshold = viewModel.triggerDetector.gyroThresholdRad,
                    onImpactThresholdChanged = { viewModel.triggerDetector.impactThresholdMs2 = it },
                    onGyroThresholdChanged = { viewModel.triggerDetector.gyroThresholdRad = it },
                    onWipeDataClicked = { viewModel.wipeAllData() }
                )
            }

            composable(Screen.Debug.route) {
                DebugScreen(
                    onSimulateCrashClicked = { viewModel.simulateCrashSequence() },
                    onManualSosClicked = { viewModel.triggerManualSos() }
                )
            }
        }
    }
}
