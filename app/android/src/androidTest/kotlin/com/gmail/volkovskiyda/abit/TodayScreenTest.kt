package com.gmail.volkovskiyda.abit

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Drives the real Activity — Koin graph, Room database and all — so this covers the wiring an
 * isolated composable test cannot: that the app starts, resolves its ViewModels and renders.
 *
 * Runs on a Gradle-managed emulator (`./gradlew ciGroupDebugAndroidTest`), so it needs no AVD set up
 * by hand and behaves the same on a laptop and on CI.
 */
@RunWith(AndroidJUnit4::class)
class TodayScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun opensOnTodayWithAllThreeDestinations() {
        // Turns every assertion below into an accessibility audit as well: a contrast or
        // touch-target regression then fails this suite rather than reaching a user. The designed
        // palette was chosen to pass it.
        composeRule.enableAccessibilityChecks()

        // "Today" is both the screen's title and its navigation label, so this asks for the first.
        composeRule.onAllNodesWithText("Today").onFirst().assertIsDisplayed()
        composeRule.onNodeWithText("Schedules").assertIsDisplayed()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun hasNoStartControlAnywhere() {
        // The one regression this product cannot afford. ABit is a schedule, not a stopwatch: a
        // "Start" button appearing here would mean the reframing the whole thing rests on has been
        // undone by accident, and it *was* there once — on the watch, mislabelling a sign-in button.
        composeRule.onNodeWithText("Start").assertDoesNotExist()
        composeRule.onNodeWithText("Stop").assertDoesNotExist()
        composeRule.onNodeWithText("Pause").assertDoesNotExist()
    }

    @Test
    fun offHoursIsTheEmptyStateRatherThanAnError() {
        // A fresh install has no schedules, and that is off hours rather than a blank screen or a
        // prompt to sign in. Sign-in is never a launch wall.
        composeRule.onNodeWithText("OFF HOURS").assertIsDisplayed()
        composeRule.onNodeWithText("Sign in with Google").assertDoesNotExist()
    }
}
