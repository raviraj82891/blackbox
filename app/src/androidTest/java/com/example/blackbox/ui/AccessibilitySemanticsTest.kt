package com.example.blackbox.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.example.blackbox.data.db.TriggerType
import com.example.blackbox.domain.trigger.CountdownState
import com.example.blackbox.ui.screens.HomeScreen
import com.example.blackbox.ui.theme.BlackboxTheme
import org.junit.Rule
import org.junit.Test

class AccessibilitySemanticsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testSosButtonHasAccessibleContentDescription() {
        var sosClicked = false

        composeTestRule.setContent {
            BlackboxTheme {
                HomeScreen(
                    isServiceRunning = true,
                    isBatterySaverActive = false,
                    batteryLevel = 85,
                    bufferEventCount = 120,
                    savedContactCount = 2,
                    isChainValid = true,
                    sparklinePoints = listOf(9.8f, 9.8f, 9.8f),
                    situationalStatus = "Stationary",
                    countdownState = CountdownState.Idle,
                    suggestAdaptiveThreshold = false,
                    safetyTimerSeconds = null,
                    onPauseResumeClicked = {},
                    onManualSosClicked = { sosClicked = true },
                    onCancelCountdownClicked = {},
                    onApplyAdaptiveThreshold = {},
                    onDismissAdaptivePrompt = {},
                    onStartSafetyTimer = {},
                    onCancelSafetyTimer = {},
                    onNavigateToContacts = {}
                )
            }
        }

        // Verify SOS button exists with explicit accessible description and responds to click
        composeTestRule
            .onNodeWithContentDescription("Emergency SOS Button. Hold for 3 seconds or tap to trigger immediate emergency alert to contacts.", substring = true)
            .assertExists()
            .performClick()

        assert(sosClicked)
    }

    @Test
    fun testCountdownOverlayHasLiveRegionSemantics() {
        composeTestRule.setContent {
            BlackboxTheme {
                HomeScreen(
                    isServiceRunning = true,
                    isBatterySaverActive = false,
                    batteryLevel = 85,
                    bufferEventCount = 120,
                    savedContactCount = 2,
                    isChainValid = true,
                    sparklinePoints = listOf(9.8f, 9.8f, 9.8f),
                    situationalStatus = "Stationary",
                    countdownState = CountdownState.ActiveCountdown(
                        secondsRemaining = 27,
                        triggerType = TriggerType.AUTO_CRASH,
                        impactMagnitude = 28.5
                    ),
                    suggestAdaptiveThreshold = false,
                    safetyTimerSeconds = null,
                    onPauseResumeClicked = {},
                    onManualSosClicked = {},
                    onCancelCountdownClicked = {},
                    onApplyAdaptiveThreshold = {},
                    onDismissAdaptivePrompt = {},
                    onStartSafetyTimer = {},
                    onCancelSafetyTimer = {},
                    onNavigateToContacts = {}
                )
            }
        }

        // Verify countdown overlay exists with live region semantic description
        composeTestRule
            .onNodeWithContentDescription("Emergency countdown active. 27 seconds remaining.", substring = true)
            .assertExists()
    }

    @Test
    fun testSystemNotificationBellHasAccessibleContentDescription() {
        composeTestRule.setContent {
            BlackboxTheme {
                HomeScreen(
                    isServiceRunning = true,
                    isBatterySaverActive = false,
                    batteryLevel = 85,
                    bufferEventCount = 120,
                    savedContactCount = 2,
                    isChainValid = true,
                    sparklinePoints = listOf(9.8f, 9.8f, 9.8f),
                    situationalStatus = "Stationary",
                    countdownState = CountdownState.Idle,
                    suggestAdaptiveThreshold = false,
                    safetyTimerSeconds = null,
                    onPauseResumeClicked = {},
                    onManualSosClicked = {},
                    onCancelCountdownClicked = {},
                    onApplyAdaptiveThreshold = {},
                    onDismissAdaptivePrompt = {},
                    onStartSafetyTimer = {},
                    onCancelSafetyTimer = {},
                    onNavigateToContacts = {}
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("System Protection Status Logs button", substring = true)
            .assertExists()
    }
}
