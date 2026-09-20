package com.example.blackbox.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.blackbox.ui.screens.OnboardingScreen
import com.example.blackbox.ui.theme.BlackboxTheme
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class OnboardingPermissionsUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testOnboardingRequiresPermissionSetupBeforeCompletion() {
        var completed = false

        composeTestRule.setContent {
            BlackboxTheme {
                OnboardingScreen(
                    onGrantPermissionsAndStart = { completed = true }
                )
            }
        }

        // 1. Advance through Splash stage
        composeTestRule.onNodeWithText("GET STARTED →", substring = true).performClick()

        // 2. Advance through Why TRACE stage
        composeTestRule.onNodeWithText("SEE HOW IT WORKS →", substring = true).performClick()

        // 3. Advance through How It Works stage
        composeTestRule.onNodeWithText("SET UP PERMISSIONS →", substring = true).performClick()

        // 4. On Permission Stage — Verify required permission header is present
        composeTestRule.onNodeWithText("PERMISSION PREREQUISITES", substring = true).assertExists()

        // Ensure onboarding completion callback is NOT triggered without permission grants
        assertFalse(completed)
    }
}
