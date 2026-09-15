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
 * isolated composable test cannot: that the app starts, resolves its ViewModel and renders.
 *
 * Runs on a Gradle-managed emulator (`./gradlew ciGroupDebugAndroidTest`), so it needs no AVD set up
 * by hand and behaves the same on a laptop and on CI.
 */
@RunWith(AndroidJUnit4::class)
class AbitAppTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun showsTheAppAndItsSyncState() {
        // Turns every assertion below into an accessibility audit as well: a contrast or
        // touch-target regression then fails this suite rather than reaching a user.
        composeRule.enableAccessibilityChecks()

        // "Today" is both the screen's title and its navigation label, so this asks for the first.
        composeRule.onAllNodesWithText("Today").onFirst().assertIsDisplayed()
        composeRule.onNodeWithText("Schedules").assertIsDisplayed()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun hasNoStartControlAnywhere() {
        // ABit is a schedule, not a stopwatch. A "Start" button appearing here would mean the
        // reframing the whole product rests on has been undone by accident.
        composeRule.onNodeWithText("Start").assertDoesNotExist()
    }
}
