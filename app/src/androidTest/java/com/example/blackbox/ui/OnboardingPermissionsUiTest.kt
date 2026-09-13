package com.example.blackbox.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.blackbox.ui.screens.OnboardingScreen
import com.example.blackbox.ui.theme.BlackboxTheme
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
        composeTestRule.onNodeWithText("Get Started", substring = true).performClick()

        // 2. Advance through Why TRACE stage
        composeTestRule.onNodeWithText("See How It Works", substring = true).performClick()

        // 3. Advance through How It Works stage
        composeTestRule.onNodeWithText("Set Up Permissions", substring = true).performClick()

        // 4. On Permission Stage — Verify required permission header is present
        composeTestRule.onNodeWithText("Set Up Required Permissions", substring = true).assertExists()

        // Ensure onboarding completion callback is NOT triggered without permission grants
        assert(!completed)
    }
}
