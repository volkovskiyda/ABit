package com.gmail.volkovskiyda.abit

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
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
    fun theHoursRowsOpenATimePicker() {
        composeRule.onNodeWithText("Schedules").performClick()
        composeRule.onNodeWithText("New schedule").performClick()

        // The rows were inert for a while: they rendered the draft's hours and swallowed the tap,
        // so a schedule could only ever run 09:00 to 18:00.
        composeRule.onNodeWithText("Starts").performClick()
        composeRule.onNodeWithText("Starts at").assertIsDisplayed()
    }

    @Test
    fun aSecondNewScheduleOpensABlankEditor() {
        composeRule.onNodeWithText("Schedules").performClick()
        composeRule.onNodeWithText("New schedule").performClick()
        composeRule.onNode(hasSetTextAction()).performTextInput("Flow test")
        composeRule.onNodeWithText("Save").performClick()
        composeRule.waitUntil { composeRule.onAllNodesWithText("RHYTHM").fetchSemanticsNodes().isEmpty() }

        composeRule.onNodeWithText("New schedule").performClick()

        // The second visit has to be its own editor. NavDisplay's default decorators scope no
        // ViewModelStore, so every destination shared the activity's: this reopened the view model
        // that had just saved, and its `saved` flag popped the destination before it drew.
        composeRule.onNodeWithText("RHYTHM").assertIsDisplayed()
        composeRule.onNodeWithText("Delete schedule").assertDoesNotExist()

        // Put the database back: TodayScreenTest asserts that a fresh install reads as off hours.
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNodeWithText("Flow test").performClick()
        composeRule.onNodeWithText("Delete schedule").performClick()
        composeRule.onNodeWithText("Delete").performClick()
        composeRule.waitUntil { composeRule.onAllNodesWithText("Flow test").fetchSemanticsNodes().isEmpty() }
    }

    @Test
    fun settingsShowsThePerDeviceCountdownSwitch() {
        composeRule.onNodeWithText("Settings").performClick()

        // The setting that is per device and never syncs — the distinction the whole product rests
        // on once four devices share one account.
        composeRule.onNodeWithText("Show countdown in notification").assertIsDisplayed()
        composeRule.onNodeWithText("PERMISSIONS").assertIsDisplayed()

        // The switch itself, not the shape that stands in for it while the preferences file is
        // being read: the screen is built fresh on every visit, and this is what proves it is built
        // from the stored value rather than from the defaults.
        composeRule.onNode(isToggleable()).assertIsOff()
    }
}
