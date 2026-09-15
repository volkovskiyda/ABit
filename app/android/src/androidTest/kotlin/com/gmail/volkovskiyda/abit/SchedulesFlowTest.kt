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
 * The path from an empty install to a schedule, through the real database. It is the flow every
 * other surface depends on: without a schedule there is nothing to chime.
 */
@RunWith(AndroidJUnit4::class)
class SchedulesFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun theSchedulesScreenOffersTheEditor() {
        composeRule.enableAccessibilityChecks()

        composeRule.onNodeWithText("Schedules").performClick()
        composeRule.onNodeWithText("New schedule").assertIsDisplayed().performClick()

        // The editor's own sections, which is how we know the FAB reached it rather than a dialog.
        composeRule.onNodeWithText("DAYS").assertIsDisplayed()
        composeRule.onNodeWithText("RHYTHM").assertIsDisplayed()
        // A blank name cannot be saved, so Save is present but the draft is invalid.
        composeRule.onNodeWithText("Save").assertIsDisplayed()
    }

    @Test
    fun settingsShowsThePerDeviceChimeSwitch() {
        composeRule.onNodeWithText("Settings").performClick()

        // The setting that is per device and never syncs — the distinction the whole product rests
        // on once four devices share one account.
        composeRule.onNodeWithText("Chime on this device").assertIsDisplayed()
        composeRule.onNodeWithText("PERMISSIONS").assertIsDisplayed()
    }
}
