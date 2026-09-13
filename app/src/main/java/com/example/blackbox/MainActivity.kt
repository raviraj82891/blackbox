package com.example.blackbox

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.blackbox.data.crypto.KeyManagementService
import com.example.blackbox.service.BlackboxForegroundService
import com.example.blackbox.service.ProtectionState
import com.example.blackbox.service.ProtectionStateManager
import com.example.blackbox.ui.navigation.BlackboxNavHost
import com.example.blackbox.ui.theme.BlackboxTheme
import com.example.blackbox.util.PermissionValidator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Automatically launch background protection service if onboarding is completed
        val kms = KeyManagementService(this)
        if (kms.isOnboardingCompleted()) {
            if (PermissionValidator.isAllRequiredGranted(this)) {
                val serviceIntent = Intent(this, BlackboxForegroundService::class.java)
                try {
                    ContextCompat.startForegroundService(this, serviceIntent)
                } catch (e: Exception) {
                    ProtectionStateManager.updateState(
                        ProtectionState.ERROR,
                        e.localizedMessage ?: "Failed to start protection service"
                    )
                }
            } else {
                val missing = PermissionValidator.getMissingRequiredPermissions(this).joinToString(", ")
                ProtectionStateManager.updateState(
                    ProtectionState.PERMISSION_LIMITED,
                    "Missing required permissions: $missing"
                )
            }
        }

        setContent {
            BlackboxTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BlackboxNavHost()
                }
            }
        }
    }
}
