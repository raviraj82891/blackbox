package com.example.blackbox.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.blackbox.data.crypto.KeyManagementService
import com.example.blackbox.data.db.TriggerType
import com.example.blackbox.ui.*
import com.example.blackbox.ui.screens.*

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Onboarding : Screen("onboarding", "Onboarding", Icons.Default.Home)
    object Home : Screen("home", "Status", Icons.Default.Home)
    object Timeline : Screen("timeline", "Timeline", Icons.AutoMirrored.Filled.List)
    object Incidents : Screen("incidents", "Reports", Icons.Default.Report)
    object Analytics : Screen("analytics", "Analytics", Icons.Default.Analytics)
    object MedicalQr : Screen("medical_qr", "Medical ID", Icons.Default.MedicalServices)
    object Contacts : Screen("contacts", "Contacts", Icons.Default.People)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun BlackboxNavHost(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val kms = remember { KeyManagementService(context) }
    val navController = rememberNavController()

    // Read onboarding completion status permanently from EncryptedSharedPreferences
    var isOnboardingCompleted by remember { mutableStateOf(kms.isOnboardingCompleted()) }

    val navItems = listOf(
        Screen.Home,
        Screen.Timeline,
        Screen.Incidents,
        Screen.Analytics,
        Screen.MedicalQr,
        Screen.Contacts,
        Screen.Settings
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
        NavHost(
            navController = navController,
            startDestination = if (isOnboardingCompleted) Screen.Home.route else Screen.Onboarding.route,
            modifier = modifier.padding(padding)
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onGrantPermissionsAndStart = {
                        kms.setOnboardingCompleted(true)
                        isOnboardingCompleted = true
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                val viewModel: HomeViewModel = hiltViewModel()
                val isServiceRunning by viewModel.isServiceRunning.collectAsState()
                val isBatterySaverActive by viewModel.isBatterySaverActive.collectAsState()
                val bufferEventCount by viewModel.bufferEventCount.collectAsState()
                val savedContactCount by viewModel.savedContactCount.collectAsState()
                val isChainValid by viewModel.isChainValid.collectAsState()
                val countdownState by viewModel.countdownState.collectAsState()
                val sparklinePoints by viewModel.sparklinePoints.collectAsState()
                val situationalStatus by viewModel.situationalStatus.collectAsState()
                val suggestAdaptiveThreshold by viewModel.suggestAdaptiveThreshold.collectAsState()
                val safetyTimerSeconds by viewModel.safetyTimerSeconds.collectAsState()

                HomeScreen(
                    isServiceRunning = isServiceRunning,
                    isBatterySaverActive = isBatterySaverActive,
                    bufferEventCount = bufferEventCount,
                    savedContactCount = savedContactCount,
                    isChainValid = isChainValid,
                    sparklinePoints = sparklinePoints,
                    situationalStatus = situationalStatus,
                    countdownState = countdownState,
                    suggestAdaptiveThreshold = suggestAdaptiveThreshold,
                    safetyTimerSeconds = safetyTimerSeconds,
                    onPauseResumeClicked = {
                        if (isServiceRunning) viewModel.pauseProtectionService() else viewModel.resumeProtectionService()
                    },
                    onWipeDataClicked = { viewModel.wipeAllData() },
                    onManualSosClicked = { viewModel.triggerManualSos() },
                    onCancelCountdownClicked = { viewModel.cancelCountdown() },
                    onApplyAdaptiveThreshold = { viewModel.applyAdaptiveThreshold() },
                    onDismissAdaptivePrompt = { viewModel.dismissAdaptivePrompt() },
                    onStartSafetyTimer = { mins -> viewModel.startSafetyCheckInTimer(mins) },
                    onCancelSafetyTimer = { viewModel.cancelSafetyCheckInTimer() },
                    onNavigateToContacts = { navController.navigate(Screen.Contacts.route) }
                )
            }

            composable(Screen.Timeline.route) {
                val viewModel: TimelineViewModel = hiltViewModel()
                val sessions by viewModel.sessions.collectAsState()
                val isChainValid by viewModel.isChainValid.collectAsState()

                TimelineScreen(
                    sessions = sessions,
                    isChainValid = isChainValid
                )
            }

            composable(Screen.Incidents.route) {
                val viewModel: IncidentsViewModel = hiltViewModel()
                val incidentReports by viewModel.incidentReports.collectAsState()

                IncidentReportScreen(
                    reports = incidentReports,
                    onUploadClicked = { report -> viewModel.uploadIncident(report) },
                    onExportPdfClicked = { report -> viewModel.generatePdfReport(report) },
                    onVerifyIntegrityClicked = { report -> viewModel.verifyIncidentIntegrity(report) }
                )
            }

            composable(Screen.Analytics.route) {
                val viewModel: AnalyticsViewModel = hiltViewModel()
                val totalEventsCount by viewModel.totalEventsCount.collectAsState()
                val autoCrashCount by viewModel.autoCrashCount.collectAsState()
                val manualSosCount by viewModel.manualSosCount.collectAsState()

                AnalyticsScreen(
                    totalEventsCount = totalEventsCount,
                    cancelledCountdownsCount = viewModel.triggerDetector.shouldSuggestThresholdAdjustment().let { 0 },
                    autoCrashCount = autoCrashCount,
                    manualSosCount = manualSosCount
                )
            }

            composable(Screen.MedicalQr.route) {
                val settingsViewModel: SettingsViewModel = hiltViewModel()
                val medicalIdData by settingsViewModel.medicalIdData.collectAsState()

                MedicalQrScreen(
                    medicalId = medicalIdData,
                    onSaveMedicalId = { settingsViewModel.saveMedicalIdData(it) }
                )
            }

            composable(Screen.Contacts.route) {
                val viewModel: ContactsViewModel = hiltViewModel()
                val contacts by viewModel.contacts.collectAsState()

                ContactsScreen(
                    contacts = contacts,
                    onAddContact = { viewModel.addContact(it) },
                    onRemoveContact = { viewModel.removeContact(it) },
                    onSendTestAlert = { viewModel.sendTestAlert(it) }
                )
            }

            composable(Screen.Settings.route) {
                val viewModel: SettingsViewModel = hiltViewModel()
                val homeViewModel: HomeViewModel = hiltViewModel()
                val isCalibrating by viewModel.isCalibrating.collectAsState()
                val calibrationProgress by viewModel.calibrationProgress.collectAsState()

                SettingsScreen(
                    impactThreshold = viewModel.triggerDetector.impactThresholdMs2,
                    gyroThreshold = viewModel.triggerDetector.gyroThresholdRad,
                    isCalibrating = isCalibrating,
                    calibrationProgress = calibrationProgress,
                    onStartCalibrationClicked = { viewModel.startPersonalCalibration() },
                    onImpactThresholdChanged = { viewModel.triggerDetector.impactThresholdMs2 = it },
                    onGyroThresholdChanged = { viewModel.triggerDetector.gyroThresholdRad = it },
                    onWipeDataClicked = { viewModel.wipeAllData() },
                    onSimulateCrashClicked = {
                        homeViewModel.triggerDetector.startCountdown(TriggerType.SIMULATED, 34.7)
                    },
                    onTestSosClicked = { homeViewModel.triggerManualSos() }
                )
            }
        }
    }
}
