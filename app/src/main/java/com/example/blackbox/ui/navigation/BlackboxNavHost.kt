package com.example.blackbox.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.blackbox.service.BlackboxForegroundService
import com.example.blackbox.service.ProtectionState
import com.example.blackbox.service.ProtectionStateManager
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.blackbox.BuildConfig
import com.example.blackbox.data.crypto.KeyManagementService
import com.example.blackbox.data.db.TriggerType
import com.example.blackbox.ui.*
import com.example.blackbox.ui.designsystem.TraceDivider
import com.example.blackbox.ui.screens.*
import com.example.blackbox.ui.theme.TraceCanvas
import com.example.blackbox.ui.theme.TraceMintSuccess
import com.example.blackbox.ui.theme.TraceMuted
import com.example.blackbox.ui.theme.TracePrimary

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Onboarding : Screen("onboarding", "Onboarding", Icons.Default.Home)
    object Home : Screen("home", "Status", Icons.Default.Home)
    object Timeline : Screen("timeline", "Timeline", Icons.AutoMirrored.Filled.List)
    object Incidents : Screen("incidents", "Incidents", Icons.Default.Report)
    object More : Screen("more", "More", Icons.Default.Menu)

    // Sub-destinations accessible from More menu or deep links
    object Analytics : Screen("analytics", "Analytics", Icons.Default.Analytics)
    object MedicalQr : Screen("medical_qr", "Medical ID", Icons.Default.MedicalServices)
    object Contacts : Screen("contacts", "Contacts", Icons.Default.People)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object Debug : Screen("debug", "Debug", Icons.Default.BugReport)
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

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    // Simplified 4-item bottom navigation
    val navItems = remember {
        listOf(
            Screen.Home,
            Screen.Timeline,
            Screen.Incidents,
            Screen.More
        )
    }

    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route ?: Screen.Home.route

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (isOnboardingCompleted && currentRoute != Screen.Onboarding.route) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    color = TraceCanvas
                ) {
                    Column {
                        TraceDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            navItems.forEach { screen ->
                                val selected = currentRoute == screen.route
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable {
                                            navController.navigate(screen.route) {
                                                popUpTo(Screen.Home.route) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = screen.label,
                                        tint = if (selected) TracePrimary else TraceMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = screen.label.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) TracePrimary else TraceMuted,
                                        letterSpacing = 1.sp
                                    )
                                    if (selected) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(16.dp)
                                                .height(2.dp)
                                                .background(TraceMintSuccess)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            val screenModifier = if (isTablet) Modifier.widthIn(max = 680.dp) else Modifier.fillMaxSize()

            NavHost(
                navController = navController,
                startDestination = if (isOnboardingCompleted) Screen.Home.route else Screen.Onboarding.route,
                modifier = screenModifier
            ) {
                composable(Screen.Onboarding.route) {
                    OnboardingScreen(
                        onGrantPermissionsAndStart = {
                            kms.setOnboardingCompleted(true)
                            isOnboardingCompleted = true
                            val serviceIntent = Intent(context, BlackboxForegroundService::class.java)
                            try {
                                ContextCompat.startForegroundService(context, serviceIntent)
                            } catch (e: Exception) {
                                ProtectionStateManager.updateState(ProtectionState.ERROR, e.localizedMessage ?: "Failed to start service")
                            }
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.Home.route) {
                    val viewModel: HomeViewModel = hiltViewModel()
                    val protectionState by viewModel.protectionState.collectAsState()
                    val protectionError by viewModel.protectionError.collectAsState()
                    val isServiceRunning by viewModel.isServiceRunning.collectAsState()
                    val isBatterySaverActive by viewModel.isBatterySaverActive.collectAsState()
                    val batteryLevel by viewModel.batteryLevel.collectAsState()
                    val bufferEventCount by viewModel.bufferEventCount.collectAsState()
                    val savedContactCount by viewModel.savedContactCount.collectAsState()
                    val isChainValid by viewModel.isChainValid.collectAsState()
                    val countdownState by viewModel.countdownState.collectAsState()
                    val lastActivatedReport by viewModel.lastActivatedReport.collectAsState()
                    val sparklinePoints by viewModel.sparklinePoints.collectAsState()
                    val situationalStatus by viewModel.situationalStatus.collectAsState()
                    val suggestAdaptiveThreshold by viewModel.suggestAdaptiveThreshold.collectAsState()
                    val safetyTimerSeconds by viewModel.safetyTimerSeconds.collectAsState()

                    HomeScreen(
                        protectionState = protectionState,
                        protectionError = protectionError,
                        isServiceRunning = isServiceRunning,
                        isBatterySaverActive = isBatterySaverActive,
                        batteryLevel = batteryLevel,
                        bufferEventCount = bufferEventCount,
                        savedContactCount = savedContactCount,
                        isChainValid = isChainValid,
                        sparklinePoints = sparklinePoints,
                        situationalStatus = situationalStatus,
                        countdownState = countdownState,
                        lastActivatedReport = lastActivatedReport,
                        suggestAdaptiveThreshold = suggestAdaptiveThreshold,
                        safetyTimerSeconds = safetyTimerSeconds,
                        onPauseResumeClicked = {
                            if (isServiceRunning) viewModel.pauseProtectionService() else viewModel.resumeProtectionService()
                        },
                        onManualSosClicked = { viewModel.triggerManualSos() },
                        onCancelCountdownClicked = { viewModel.cancelCountdown() },
                        onClearLastActivatedReport = { viewModel.clearLastActivatedReport() },
                        onApplyAdaptiveThreshold = { viewModel.applyAdaptiveThreshold() },
                        onDismissAdaptivePrompt = { viewModel.dismissAdaptivePrompt() },
                        onStartSafetyTimer = { mins -> viewModel.startSafetyCheckInTimer(mins) },
                        onCancelSafetyTimer = { viewModel.cancelSafetyCheckInTimer() },
                        onNavigateToContacts = { navController.navigate(Screen.Contacts.route) },
                        onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                        onNavigateToIncidents = { navController.navigate(Screen.Incidents.route) }
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

                composable(Screen.More.route) {
                    val homeViewModel: HomeViewModel = hiltViewModel()
                    val savedContactCount by homeViewModel.savedContactCount.collectAsState()

                    MoreScreen(
                        savedContactCount = savedContactCount,
                        onNavigateToContacts = { navController.navigate(Screen.Contacts.route) },
                        onNavigateToMedicalId = { navController.navigate(Screen.MedicalQr.route) },
                        onNavigateToAnalytics = { navController.navigate(Screen.Analytics.route) },
                        onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                        onNavigateToDebug = { navController.navigate(Screen.Debug.route) }
                    )
                }

                composable(Screen.Analytics.route) {
                    val viewModel: AnalyticsViewModel = hiltViewModel()
                    val analyticsState by viewModel.analyticsState.collectAsState()

                    AnalyticsScreen(
                        state = analyticsState
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
                        onUpdateContact = { viewModel.updateContact(it) },
                        onRemoveContact = { viewModel.removeContact(it) },
                        onSetPrimaryContact = { viewModel.setPrimaryContact(it) },
                        onPreviewAlertOnThisPhone = { viewModel.previewAlertOnThisPhone(it) },
                        onSendRealTestAlert = { contact -> viewModel.sendRealTestAlertToContact(contact) }
                    )
                }

                composable(Screen.Settings.route) {
                    val viewModel: SettingsViewModel = hiltViewModel()
                    val homeViewModel: HomeViewModel = hiltViewModel()
                    val isCalibrating by viewModel.isCalibrating.collectAsState()
                    val calibrationProgress by viewModel.calibrationProgress.collectAsState()
                    val calibrationResultMessage by viewModel.calibrationResultMessage.collectAsState()
                    val isAutoDetectionEnabled by viewModel.isAutoDetectionEnabled.collectAsState()

                    SettingsScreen(
                        isAutoDetectionEnabled = isAutoDetectionEnabled,
                        onAutoDetectionChanged = { viewModel.setAutoDetectionEnabled(it) },
                        impactThreshold = viewModel.triggerDetector.impactThresholdMs2,
                        gyroThreshold = viewModel.triggerDetector.gyroThresholdRad,
                        isCalibrating = isCalibrating,
                        calibrationProgress = calibrationProgress,
                        calibrationResultMessage = calibrationResultMessage,
                        onStartCalibrationClicked = { viewModel.startPersonalCalibration() },
                        onImpactThresholdChanged = { viewModel.triggerDetector.impactThresholdMs2 = it },
                        onGyroThresholdChanged = { viewModel.triggerDetector.gyroThresholdRad = it },
                        onClearBufferClicked = { viewModel.clearSensorBuffer() },
                        onDeleteIncidentsClicked = { viewModel.deleteIncidentHistory() },
                        onFactoryResetClicked = {
                            viewModel.wipeAllData {
                                kms.setOnboardingCompleted(false)
                                isOnboardingCompleted = false
                                navController.navigate(Screen.Onboarding.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        },
                        onSimulateCrashClicked = {
                            homeViewModel.triggerDetector.startCountdown(TriggerType.SIMULATED, 34.7)
                        },
                        onTestSosClicked = { homeViewModel.triggerManualSos() },
                        onNavigateToContacts = { navController.navigate(Screen.Contacts.route) },
                        onNavigateToMedicalId = { navController.navigate(Screen.MedicalQr.route) }
                    )
                }

                // Expose DebugScreen route ONLY in Debug builds
                if (BuildConfig.DEBUG) {
                    composable(Screen.Debug.route) {
                        val homeViewModel: HomeViewModel = hiltViewModel()

                        DebugScreen(
                            onSimulateCrashClicked = {
                                homeViewModel.triggerDetector.startCountdown(TriggerType.SIMULATED, 34.7)
                            },
                            onManualSosClicked = { homeViewModel.triggerManualSos() }
                        )
                    }
                }
            }
        }
    }
}
