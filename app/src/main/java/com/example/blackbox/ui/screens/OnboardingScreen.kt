package com.example.blackbox.ui.screens

import android.Manifest
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OnboardingScreen(
    onGrantPermissionsAndStart: () -> Unit
) {
    var onboardingStage by remember { mutableIntStateOf(0) } // 0: Vision & Privacy, 1: Walkthrough, 2: Staged Permissions

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            when (onboardingStage) {
                0 -> VisionAndPrivacyStage(onNextClicked = { onboardingStage = 1 })
                1 -> WalkthroughStage(onNextClicked = { onboardingStage = 2 })
                2 -> StagedPermissionsStage(onComplete = onGrantPermissionsAndStart)
            }
        }
    }
}

@Composable
private fun VisionAndPrivacyStage(onNextClicked: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(
                Icons.Default.Shield,
                contentDescription = null,
                modifier = Modifier
                    .padding(24.dp)
                    .size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "TRACE — Digital Black Box",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "If you're ever in a serious accident and can't explain what happened, TRACE already has the last 60 minutes recorded — automatically, safely, and only for you.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        PrivacyPrincipleCard(
            title = "We throw away anything older than an hour",
            description = "Your phone keeps a rolling 60-minute safety buffer. Older data is deleted permanently and automatically.",
            icon = Icons.Default.Lock
        )

        Spacer(modifier = Modifier.height(12.dp))

        PrivacyPrincipleCard(
            title = "We never save what you actually say",
            description = "Microphone sound is checked in RAM only to recognize loud acoustic impacts. Raw voice recordings are discarded immediately.",
            icon = Icons.Default.Mic
        )

        Spacer(modifier = Modifier.height(12.dp))

        PrivacyPrincipleCard(
            title = "Only you choose who can read a report",
            description = "Incident reports are encrypted on your phone. Cloud servers never hold your unencrypted data or keys.",
            icon = Icons.Default.CheckCircle
        )

        Spacer(modifier = Modifier.height(12.dp))

        PrivacyPrincipleCard(
            title = "Nobody can quietly alter your timeline",
            description = "Every log entry is cryptographically sealed so nobody can modify what happened after an incident.",
            icon = Icons.Default.Shield
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onNextClicked,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("See How It Works", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun WalkthroughStage(onNextClicked: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "How TRACE Protects You",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        WalkthroughStepCard(
            stepNumber = "1",
            title = "Continuous Silent Protection",
            description = "TRACE quietly monitors motion, location, and environmental status over a rolling 60-minute window."
        )

        Spacer(modifier = Modifier.height(14.dp))

        WalkthroughStepCard(
            stepNumber = "2",
            title = "Smart Incident Detection",
            description = "If a crash or severe fall occurs, TRACE gives you 30 seconds to tap 'I'm OK' before help is called."
        )

        Spacer(modifier = Modifier.height(14.dp))

        WalkthroughStepCard(
            stepNumber = "3",
            title = "Instant Emergency Dispatch",
            description = "If you don't cancel, your trusted contacts receive an encrypted timeline report and your exact location link."
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onNextClicked,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Set Up Permissions", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StagedPermissionsStage(onComplete: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }

    val singlePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        if (step < 3) {
            step++
        } else {
            onComplete()
        }
    }

    val multiplePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (step < 3) {
            step++
        } else {
            onComplete()
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "One-Time Permission Setup",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "We explain every permission before asking so you know exactly why TRACE needs it.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        when (step) {
            0 -> PermissionExplanationCard(
                icon = Icons.Default.MyLocation,
                title = "Location Permission",
                explanation = "This lets TRACE record where an incident happened so emergency responders or family know where you are.",
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
            1 -> PermissionExplanationCard(
                icon = Icons.Default.Mic,
                title = "Microphone Permission",
                explanation = "This allows on-device acoustic detection of sudden loud impacts. Raw voice recordings are NEVER saved.",
                buttonText = "Grant Microphone Permission",
                onRequest = {
                    singlePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            )
            2 -> PermissionExplanationCard(
                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                title = "Physical Activity Permission",
                explanation = "This allows TRACE to know if you were walking, driving, or stationary prior to an emergency.",
                buttonText = "Grant Activity Permission",
                onRequest = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        singlePermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                    } else {
                        step++
                    }
                }
            )
            3 -> PermissionExplanationCard(
                icon = Icons.Default.Notifications,
                title = "Notifications Permission",
                explanation = "This shows a subtle background notification letting you know TRACE protection is active.",
                buttonText = "Enable Notifications & Finish",
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
private fun PermissionExplanationCard(
    icon: ImageVector,
    title: String,
    explanation: String,
    buttonText: String,
    onRequest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(explanation, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onRequest,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(buttonText, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun WalkthroughStepCard(stepNumber: String, title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(stepNumber, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(2.dp))
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PrivacyPrincipleCard(
    title: String,
    description: String,
    icon: ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(26.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
