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
import com.example.blackbox.ui.navigation.BlackboxNavHost
import com.example.blackbox.ui.theme.BlackboxTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Automatically launch background protection service if onboarding is completed
        val kms = KeyManagementService(this)
        if (kms.isOnboardingCompleted()) {
            val serviceIntent = Intent(this, BlackboxForegroundService::class.java)
            try {
                ContextCompat.startForegroundService(this, serviceIntent)
            } catch (e: Exception) {
                // Background start fallback
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
