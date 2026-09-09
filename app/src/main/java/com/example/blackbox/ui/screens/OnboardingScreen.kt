package com.example.blackbox.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.core.content.ContextCompat
import com.example.blackbox.ui.theme.*

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
    var step by remember { mutableIntStateOf(0) }

    fun checkIsGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(step) {
        when (step) {
            0 -> if (checkIsGranted(Manifest.permission.ACCESS_FINE_LOCATION)) step = 1
            1 -> if (checkIsGranted(Manifest.permission.RECORD_AUDIO)) step = 2
            2 -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (checkIsGranted(Manifest.permission.ACTIVITY_RECOGNITION)) step = 3 else Unit
            } else {
                step = 3
            }
            3 -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (checkIsGranted(Manifest.permission.POST_NOTIFICATIONS)) onComplete() else Unit
            } else {
                onComplete()
            }
        }
    }

    val singlePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        if (step < 3) step++ else onComplete()
    }

    val multiplePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (step < 3) step++ else onComplete()
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
            text = "Let's set up TRACE",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "We'll explain each permission so you know exactly why it's needed.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MutedSlate
        )

        Spacer(modifier = Modifier.height(24.dp))

        when (step) {
            0 -> PermissionCard(
                icon = Icons.Default.MyLocation,
                title = "Location Permission",
                explanation = "This helps TRACE record where an incident happens so emergency responders or family know where you are.",
                bullets = listOf("Only stored on your device", "Used only for safety purposes", "Adaptive tracking to save battery"),
                buttonText = "Grant Location Permission",
                onRequest = {
                    multiplePermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            )
            1 -> PermissionCard(
                icon = Icons.Default.Mic,
                title = "Microphone Permission",
                explanation = "This allows TRACE to know if an acoustic impact occurred during a crash. Raw audio is never stored.",
                bullets = listOf("Analyzed frame-by-frame in RAM", "No raw voice recordings saved", "Zero audio cloud upload"),
                buttonText = "Grant Microphone Permission",
                onRequest = {
                    singlePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            )
            2 -> PermissionCard(
                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                title = "Physical Activity Permission",
                explanation = "This allows TRACE to know if you were walking, driving, or stationary prior to an emergency.",
                bullets = listOf("Helps detect real incidents", "Improves timeline accuracy", "No fitness data shared"),
                buttonText = "Grant Activity Permission",
                onRequest = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        singlePermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                    } else {
                        step = 3
                    }
                }
            )
            3 -> PermissionCard(
                icon = Icons.Default.Notifications,
                title = "Notifications Permission",
                explanation = "This lets us show a subtle background notification so you know TRACE is active.",
                bullets = listOf("Helps protection status", "Important safety alerts", "Instant SOS feedback"),
                buttonText = "Enable Notifications",
                onRequest = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        singlePermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        onComplete()
                    }
                }
            )
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    explanation: String,
    bullets: List<String>,
    buttonText: String,
    onRequest: () -> Unit
) {
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
            Surface(
                shape = CircleShape,
                color = SoftGreenContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = Mint, modifier = Modifier.size(32.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(explanation, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MutedSlate)

            Spacer(modifier = Modifier.height(16.dp))

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

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onRequest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Mint)
            ) {
                Text(buttonText, fontWeight = FontWeight.Bold, color = Color.White)
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
