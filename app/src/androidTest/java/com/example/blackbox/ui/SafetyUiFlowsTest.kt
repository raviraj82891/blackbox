package com.example.blackbox.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.blackbox.service.ProtectionState
import com.example.blackbox.ui.screens.HomeScreen
import com.example.blackbox.ui.theme.BlackboxTheme
import org.junit.Rule
import org.junit.Test

class SafetyUiFlowsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testHomeScreenRendersProtectionActiveStateAndEmergencySos() {
        composeTestRule.setContent {
            BlackboxTheme {
                HomeScreen(
                    protectionState = ProtectionState.ACTIVE,
                    isServiceRunning = true,
                    batteryLevel = 85,
                    bufferEventCount = 142,
                    savedContactCount = 2,
                    isChainValid = true
                )
            }
        }

        // Verify Protection Active header and accessibility semantics
        composeTestRule.onNodeWithText("PROTECTION ACTIVE", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("SOS EMERGENCY ALERT", substring = true).assertIsDisplayed()
    }

    @Test
    fun testHomeScreenRendersProtectionLimitedStateWhenContactsMissing() {
        composeTestRule.setContent {
            BlackboxTheme {
                HomeScreen(
                    protectionState = ProtectionState.PERMISSION_LIMITED,
                    protectionError = "Missing required permissions: Microphone",
                    isServiceRunning = false,
                    batteryLevel = 12,
                    bufferEventCount = 0,
                    savedContactCount = 0,
                    isChainValid = null
                )
            }
        }

        // Verify Protection Limited header
        composeTestRule.onNodeWithText("PROTECTION LIMITED", substring = true).assertIsDisplayed()
    }
}
