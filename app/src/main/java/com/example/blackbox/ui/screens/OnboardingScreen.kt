package com.example.blackbox.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
    var stage by remember { mutableIntStateOf(0) } // 0: Splash, 1: Why TRACE, 2: How It Works, 3: Permissions

    Scaffold(containerColor = OffWhite) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
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
        Spacer(modifier = Modifier.height(10.dp))

        // TRACE Shield Header
        Surface(
            shape = CircleShape,
            color = Mint,
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(text = "TRACE", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
        Text(text = "Your Story. Protected.", style = MaterialTheme.typography.bodyMedium, color = MutedSlate)

        Spacer(modifier = Modifier.height(24.dp))

        // Hero Illustration Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF2DD4BF), Color(0xFF0F172A))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(20.dp)
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = Color.White, modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Peace of mind for a safer tomorrow.",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        fontSize = 18.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Dark Pill Banner
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "A digital black box for your life.",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Automatically records. Protects your privacy. Stands with you when it matters.",
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Mint)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Get Started", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = null, tint = Color.White)
            }
        }
    }
}

@Composable
private fun WhyTraceStage(onNext: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Why TRACE?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Real protection. Real privacy.",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedSlate
        )

        Spacer(modifier = Modifier.height(24.dp))

        PrivacyFeatureCard(
            title = "Keeps only the last hour",
            description = "Older data is deleted automatically.",
            icon = Icons.Default.Schedule,
            containerColor = SoftGreenContainer,
            iconTint = Mint
        )

        Spacer(modifier = Modifier.height(12.dp))

        PrivacyFeatureCard(
            title = "We never save what you say",
            description = "Audio is analysed in real-time. No raw recordings are stored.",
            icon = Icons.Default.Mic,
            containerColor = SoftCoralContainer,
            iconTint = WarmCoral
        )

        Spacer(modifier = Modifier.height(12.dp))

        PrivacyFeatureCard(
            title = "You control who can access",
            description = "Reports are encrypted. Only your trusted contacts can read them.",
            icon = Icons.Default.Group,
            containerColor = SoftBlueContainer,
            iconTint = SoftTeal
        )

        Spacer(modifier = Modifier.height(12.dp))

        PrivacyFeatureCard(
            title = "Tamper-evident & trustworthy",
            description = "Every event is cryptographically sealed for post-incident integrity verification.",
            icon = Icons.Default.Shield,
            containerColor = SoftOrangeContainer,
            iconTint = Peach
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Mint)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("See How It Works", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = null, tint = Color.White)
            }
        }
    }
}

@Composable
private fun HowItWorksStage(onNext: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "How TRACE Protects You",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        StepRowCard(
            stepNumber = "1",
            title = "Continuous Monitoring",
            description = "Sensors quietly record motion, location and environment over a rolling 60-minute window.",
            badgeColor = Mint
        )

        Spacer(modifier = Modifier.height(16.dp))

        StepRowCard(
            stepNumber = "2",
            title = "Smart Detection",
            description = "If a crash or severe fall occurs, you get 30 seconds to confirm you're OK.",
            badgeColor = WarmCoral
        )

        Spacer(modifier = Modifier.height(16.dp))

        StepRowCard(
            stepNumber = "3",
            title = "Emergency Dispatch",
            description = "If not canceled, your trusted contacts receive an encrypted report and your location.",
            badgeColor = SoftTeal
        )

        Spacer(modifier = Modifier.height(36.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Mint)
        ) {
            Text("Set Up Permissions", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun StagedPermissionsStage(onComplete: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    var step by remember { mutableIntStateOf(0) }

    var stepStatus by remember { mutableStateOf(PermissionStepStatus.NOT_REQUESTED) }

    // Synchronize current step status with actual permission state
    fun syncCurrentStepStatus() {
        val isGranted = when (step) {
            0 -> PermissionValidator.hasLocationPermission(context)
            1 -> PermissionValidator.hasMicPermission(context)
            2 -> PermissionValidator.hasActivityPermission(context)
            3 -> PermissionValidator.hasNotificationPermission(context)
            else -> false
        }
        if (isGranted) {
            stepStatus = PermissionStepStatus.GRANTED
        }
    }

    LaunchedEffect(step) {
        syncCurrentStepStatus()
    }

    // Permission Launchers
    val singleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            stepStatus = PermissionStepStatus.GRANTED
            if (step < 3) step++ else {
                if (PermissionValidator.isAllRequiredGranted(context)) onComplete()
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
        // Stepper dots indicator (1/4 .. 4/4)
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            for (i in 0..3) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (i == step) 10.dp else 8.dp)
                        .background(
                            color = if (i == step) Mint else MutedSlate.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("${step + 1}/4", style = MaterialTheme.typography.labelMedium, color = MutedSlate)
        }

        Text(
            text = "Set Up Required Permissions",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Required permissions must be granted to activate TRACE safety monitoring.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MutedSlate
        )

        Spacer(modifier = Modifier.height(20.dp))

        when (step) {
            0 -> PermissionCard(
                icon = Icons.Default.MyLocation,
                title = "Location Permission",
                isRequired = true,
                status = stepStatus,
                explanation = "This helps TRACE record where an incident happens so emergency responders or family know where you are.",
                consequenceText = "Without location permission, TRACE cannot record GPS coordinates during a crash.",
                bullets = listOf("Only stored locally on device", "Used only for safety dispatches", "Adaptive battery-friendly tracking"),
                buttonText = "Grant Location Permission",
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
                icon = Icons.Default.Mic,
                title = "Microphone Permission",
                isRequired = true,
                status = stepStatus,
                explanation = "This allows TRACE to know if an acoustic impact occurred during a crash. Raw audio is never stored.",
                consequenceText = "Without microphone permission, TRACE cannot classify loud acoustic impact events.",
                bullets = listOf("Analyzed frame-by-frame in RAM", "No raw voice recordings saved", "Zero audio cloud uploads"),
                buttonText = "Grant Microphone Permission",
                onRequest = {
                    singleLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
                onNext = { step = 2 }
            )
            2 -> PermissionCard(
                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                title = "Physical Activity Permission",
                isRequired = true,
                status = stepStatus,
                explanation = "This allows TRACE to know if you were walking, driving, or stationary prior to an emergency.",
                consequenceText = "Without activity permission, TRACE cannot detect motion transitions or auto-adjust location intervals.",
                bullets = listOf("Helps detect real crash/fall incidents", "Improves timeline accuracy", "No fitness data shared"),
                buttonText = "Grant Activity Permission",
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
                icon = Icons.Default.Notifications,
                title = "Notifications Permission",
                isRequired = false,
                status = stepStatus,
                explanation = "This lets us show a subtle background notification so you know TRACE is active.",
                consequenceText = "Without notification permission, background status warnings won't appear in your shade.",
                bullets = listOf("Shows active monitoring status", "Important safety alerts", "Instant SOS feedback"),
                buttonText = "Enable Notifications",
                onRequest = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        singleLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        stepStatus = PermissionStepStatus.GRANTED
                        if (PermissionValidator.isAllRequiredGranted(context)) onComplete()
                    }
                },
                onNext = {
                    if (PermissionValidator.isAllRequiredGranted(context)) onComplete()
                }
            )
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    isRequired: Boolean,
    status: PermissionStepStatus,
    explanation: String,
    consequenceText: String,
    bullets: List<String>,
    buttonText: String,
    onRequest: () -> Unit,
    onNext: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isRequired) SoftCoralContainer else SoftBlueContainer
                ) {
                    Text(
                        text = if (isRequired) "Required" else "Optional",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isRequired) WarmCoral else DarkTeal,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (status == PermissionStepStatus.GRANTED) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SoftGreenContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Mint, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Granted", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Mint)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = CircleShape,
                color = if (status == PermissionStepStatus.GRANTED) SoftGreenContainer else SoftCoralContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (status == PermissionStepStatus.GRANTED) Mint else WarmCoral,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(explanation, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MutedSlate)

            Spacer(modifier = Modifier.height(14.dp))

            // Denied Consequence Warning Card
            if (status == PermissionStepStatus.DENIED || status == PermissionStepStatus.PERMANENTLY_DENIED) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = SoftCoralContainer
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = WarmCoral, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(consequenceText, fontSize = 12.sp, color = CharcoalText, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            bullets.forEach { bullet ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Mint, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(bullet, style = MaterialTheme.typography.bodySmall, color = CharcoalText)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            when (status) {
                PermissionStepStatus.GRANTED -> {
                    Button(
                        onClick = onNext,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Mint)
                    ) {
                        Text("Continue", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                PermissionStepStatus.PERMANENTLY_DENIED -> {
                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WarmCoral)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open App Settings", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                else -> {
                    Button(
                        onClick = onRequest,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WarmCoral)
                    ) {
                        Text(buttonText, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    if (!isRequired) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = onNext) {
                            Text("Skip Optional Permission", color = MutedSlate)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepRowCard(stepNumber: String, title: String, description: String, badgeColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = badgeColor,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(stepNumber, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(2.dp))
                Text(description, style = MaterialTheme.typography.bodySmall, color = MutedSlate)
            }
        }
    }
}

@Composable
private fun PrivacyFeatureCard(
    title: String,
    description: String,
    icon: ImageVector,
    containerColor: Color,
    iconTint: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = containerColor,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = MutedSlate)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MutedSlate)
        }
    }
}
