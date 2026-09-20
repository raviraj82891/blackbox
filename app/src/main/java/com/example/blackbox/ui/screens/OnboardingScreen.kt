package com.example.blackbox.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.blackbox.ui.designsystem.*
import com.example.blackbox.ui.theme.*
import com.example.blackbox.util.PermissionValidator

enum class PermissionStepStatus {
    NOT_REQUESTED,
    GRANTED,
    DENIED,
    PERMANENTLY_DENIED
}

@Composable
fun OnboardingScreen(
    onGrantPermissionsAndStart: () -> Unit
) {
    var stage by remember { mutableIntStateOf(0) }

    Scaffold(containerColor = TraceCanvas) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (stage) {
                0 -> HeroSplashStage(onNext = { stage = 1 })
                1 -> WhyTraceStage(onNext = { stage = 2 })
                2 -> HowItWorksStage(onNext = { stage = 3 })
                3 -> StagedPermissionsStage(onComplete = onGrantPermissionsAndStart)
            }
        }
    }
}

@Composable
private fun HeroSplashStage(onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "TRACE",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = TracePrimary,
            letterSpacing = 4.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "YOUR PERSONAL DIGITAL BLACK BOX",
            style = MaterialTheme.typography.labelLarge,
            color = TraceMuted,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, TraceHairline, RectangleShape),
            color = TraceSurface
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "A CONTINUOUS SAFETY BUFFER FOR YOUR LIFE",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TracePrimary,
                    textAlign = TextAlign.Center,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Automatically records 60-minute rolling sensor telemetry. Protects privacy. Dispatches encrypted alerts when it matters.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TraceMuted,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        TracePrimaryButton(
            text = "GET STARTED →",
            onClick = onNext
        )
    }
}

@Composable
private fun WhyTraceStage(onNext: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(16.dp))

        TraceSectionHeader(title = "PRIVACY FIRST SPECIFICATION")

        Text(
            text = "REAL PROTECTION. REAL PRIVACY.",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TracePrimary,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        TraceSpecRow(label = "DATA RETENTION", value = "ROLLING 60 MINUTES ONLY")
        TraceSpecRow(label = "AUDIO MONITORING", value = "REAL-TIME RAM CLASSIFICATION (NO RAW AUDIO)")
        TraceSpecRow(label = "ACCESS CONTROL", value = "CLIENT-SIDE AES-256 ENCRYPTED")
        TraceSpecRow(label = "INTEGRITY", value = "SHA-256 HASH CHAIN SEALED")

        Spacer(modifier = Modifier.height(48.dp))

        TracePrimaryButton(
            text = "SEE HOW IT WORKS →",
            onClick = onNext
        )
    }
}

@Composable
private fun HowItWorksStage(onNext: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(16.dp))

        TraceSectionHeader(title = "HOW TRACE PROTECTS YOU")

        Spacer(modifier = Modifier.height(16.dp))

        TraceSpecRow(label = "01 CONTINUOUS MONITORING", value = "QUIET 60-MIN SENSOR BUFFER")
        TraceSpecRow(label = "02 SMART DETECTION", value = "AUTOMATIC CRASH / FALL SENSING")
        TraceSpecRow(label = "03 EMERGENCY DISPATCH", value = "ENCRYPTED CONTACT NOTIFICATION")

        Spacer(modifier = Modifier.height(48.dp))

        TracePrimaryButton(
            text = "SET UP PERMISSIONS →",
            onClick = onNext
        )
    }
}

@Composable
private fun StagedPermissionsStage(onComplete: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context as? Activity
    var step by remember { mutableIntStateOf(0) }

    // Re-verify actual PackageManager permission state per step
    fun checkStepPermission(currentStep: Int): Boolean {
        return when (currentStep) {
            0 -> PermissionValidator.hasLocationPermission(context)
            1 -> PermissionValidator.hasMicPermission(context)
            2 -> PermissionValidator.hasActivityPermission(context)
            3 -> PermissionValidator.hasNotificationPermission(context)
            else -> false
        }
    }

    var stepStatus by remember(step) {
        mutableStateOf(
            if (checkStepPermission(step)) PermissionStepStatus.GRANTED
            else PermissionStepStatus.NOT_REQUESTED
        )
    }

    // Refresh permission state whenever app resumes (e.g. returning from Android Settings)
    DisposableEffect(lifecycleOwner, step) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (checkStepPermission(step)) {
                    stepStatus = PermissionStepStatus.GRANTED
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val singleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            stepStatus = PermissionStepStatus.GRANTED
            if (step < 3) {
                step++
            } else {
                onComplete()
            }
        } else {
            val perm = when (step) {
                1 -> Manifest.permission.RECORD_AUDIO
                2 -> Manifest.permission.ACTIVITY_RECOGNITION
                3 -> Manifest.permission.POST_NOTIFICATIONS
                else -> ""
            }
            val showRationale = activity != null && perm.isNotBlank() && ActivityCompat.shouldShowRequestPermissionRationale(activity, perm)
            stepStatus = if (showRationale) PermissionStepStatus.DENIED else PermissionStepStatus.PERMANENTLY_DENIED
        }
    }

    val multipleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { map ->
        val fine = map[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarse = map[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fine || coarse) {
            stepStatus = PermissionStepStatus.GRANTED
            step = 1
        } else {
            val showRationale = activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)
            stepStatus = if (showRationale) PermissionStepStatus.DENIED else PermissionStepStatus.PERMANENTLY_DENIED
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(16.dp))

        TraceSectionHeader(title = "PERMISSION PREREQUISITES (${step + 1}/4)")

        Spacer(modifier = Modifier.height(12.dp))

        when (step) {
            0 -> PermissionCard(
                title = "LOCATION PERMISSION",
                isRequired = true,
                status = stepStatus,
                explanation = "Allows TRACE to record incident coordinates so emergency responders or family know your location.",
                consequenceText = "Without location permission, TRACE cannot record GPS coordinates during a crash.",
                buttonText = "GRANT LOCATION PERMISSION",
                onRequest = {
                    multipleLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                onNext = { step = 1 }
            )
            1 -> PermissionCard(
                title = "MICROPHONE PERMISSION",
                isRequired = true,
                status = stepStatus,
                explanation = "Allows TRACE to detect acoustic impact noise during severe crashes. Raw audio is never saved.",
                consequenceText = "Without microphone permission, TRACE cannot classify loud acoustic impact events.",
                buttonText = "GRANT MICROPHONE PERMISSION",
                onRequest = {
                    singleLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
                onNext = { step = 2 }
            )
            2 -> PermissionCard(
                title = "PHYSICAL ACTIVITY PERMISSION",
                isRequired = true,
                status = stepStatus,
                explanation = "Allows TRACE to know if you were walking, driving, or stationary prior to an emergency.",
                consequenceText = "Without activity permission, TRACE cannot detect motion transitions.",
                buttonText = "GRANT ACTIVITY PERMISSION",
                onRequest = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        singleLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                    } else {
                        stepStatus = PermissionStepStatus.GRANTED
                        step = 3
                    }
                },
                onNext = { step = 3 }
            )
            3 -> PermissionCard(
                title = "NOTIFICATIONS PERMISSION",
                isRequired = false,
                status = stepStatus,
                explanation = "Shows background service status and instant feedback during emergency countdowns.",
                consequenceText = "Without notification permission, background status warnings won't appear.",
                buttonText = "ENABLE NOTIFICATIONS",
                onRequest = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        singleLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        stepStatus = PermissionStepStatus.GRANTED
                        onComplete()
                    }
                },
                onNext = {
                    onComplete()
                }
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun PermissionCard(
    title: String,
    isRequired: Boolean,
    status: PermissionStepStatus,
    explanation: String,
    consequenceText: String,
    buttonText: String,
    onRequest: () -> Unit,
    onNext: () -> Unit
) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, TraceHairline, RectangleShape),
        color = TraceSurface
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TraceBadge(
                    text = if (isRequired) "REQUIRED" else "OPTIONAL",
                    color = if (isRequired) TraceAmberWarning else TraceBlueInfo
                )

                if (status == PermissionStepStatus.GRANTED) {
                    TraceBadge(text = "GRANTED", color = TraceMintSuccess)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TracePrimary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = explanation,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = TraceMuted
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (status == PermissionStepStatus.DENIED || status == PermissionStepStatus.PERMANENTLY_DENIED) {
                Text(
                    text = consequenceText.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = TraceRedCritical,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            when (status) {
                PermissionStepStatus.GRANTED -> {
                    TracePrimaryButton(
                        text = "CONTINUE →",
                        onClick = onNext
                    )
                }
                PermissionStepStatus.PERMANENTLY_DENIED -> {
                    TracePrimaryButton(
                        text = "OPEN APP SETTINGS",
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        },
                        isCritical = true
                    )
                }
                else -> {
                    TracePrimaryButton(
                        text = buttonText,
                        onClick = onRequest
                    )

                    if (!isRequired) {
                        Spacer(modifier = Modifier.height(12.dp))
                        TraceSecondaryButton(
                            text = "SKIP OPTIONAL PERMISSION",
                            onClick = onNext
                        )
                    }
                }
            }
        }
    }
}
