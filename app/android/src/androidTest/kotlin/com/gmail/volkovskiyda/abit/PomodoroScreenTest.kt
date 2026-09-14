package com.gmail.volkovskiyda.abit

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Drives the real Activity — Koin graph, Room database and all — so this covers the wiring an
 * isolated composable test cannot: that the app starts, resolves its ViewModel and renders.
 *
 * Runs on a Gradle-managed emulator (`./gradlew ciGroupDebugAndroidTest`), so it needs no AVD set up
 * by hand and behaves the same on a laptop and on CI.
 */
@RunWith(AndroidJUnit4::class)
class PomodoroScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun showsTheAppAndItsSyncState() {
        // Turns every assertion below into an accessibility audit as well: a contrast or
        // touch-target regression then fails this suite rather than reaching a user.
        composeRule.enableAccessibilityChecks()

        composeRule.onNodeWithText("ABit").assertIsDisplayed()
        composeRule.onNodeWithText("Sessions: 0").assertIsDisplayed()
    }

    @Test
    fun offersAnAccountFreeStart() {
        // Anonymous sign-in is the product's front door: someone must be able to use ABit without
        // an account at all.
        composeRule.onNodeWithText("Use without an account").assertIsDisplayed().performClick()
    }
}
