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
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gmail.volkovskiyda.abit.core.domain.ScheduleRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

/**
 * The path from an empty install to a schedule, through the real database. It is the flow every
 * other surface depends on: without a schedule there is nothing to chime.
 *
 * Every assertion on a section of the editor scrolls to it first. The editor is one scrolling
 * column and the shortest device in the Test Lab matrix is 640 dp tall, where RHYTHM sits below the
 * fold the moment anything above it grows — the overlap warning a second schedule draws is enough.
 * An assertion that assumes a section is on screen tests the device, not the app.
 */
@RunWith(AndroidJUnit4::class)
class SchedulesFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    /**
     * The database is the one thing these tests share with every other class in the suite, and
     * `TodayScreenTest` asserts that a fresh install reads as off hours. Restoring it through the UI
     * at the end of a test body only works while that body passes: a failing assertion leaves the
     * schedule behind and fails the *next* class as well, which is how one broken layout assertion
     * cost two tests in Test Lab. This runs either way.
     */
    @After
    fun clearSchedules() =
        runBlocking {
            val repository = GlobalContext.get().get<ScheduleRepository>()
            repository.observeSchedules().first().forEach { repository.delete(it.id) }
        }

    @Test
    fun theSchedulesScreenOffersTheEditor() {
        composeRule.enableAccessibilityChecks()

        composeRule.onNodeWithText("Schedules").performClick()
        composeRule.onNodeWithText("New schedule").assertIsDisplayed().performClick()

        // The editor's own sections, which is how we know the FAB reached it rather than a dialog.
        composeRule.onNodeWithText("DAYS").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("RHYTHM").performScrollTo().assertIsDisplayed()
        // A blank name cannot be saved, so Save is present but the draft is invalid. It rides in the
        // top bar rather than the scrolling column, so it is on screen at any height.
        composeRule.onNodeWithText("Save").assertIsDisplayed()
    }

    @Test
    fun theHoursRowsOpenATimePicker() {
        composeRule.onNodeWithText("Schedules").performClick()
        composeRule.onNodeWithText("New schedule").performClick()

        // The rows were inert for a while: they rendered the draft's hours and swallowed the tap,
        // so a schedule could only ever run 09:00 to 18:00.
        composeRule.onNodeWithText("Starts").performScrollTo().performClick()
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
        composeRule.onNodeWithText("RHYTHM").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Delete schedule").assertDoesNotExist()

        // The delete flow, on the schedule just saved: the dialog is the only place the error colour
        // is allowed to appear, and the row has to leave the list. `clearSchedules` covers the
        // database either way, so this is coverage rather than housekeeping.
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNodeWithText("Flow test").performClick()
        composeRule.onNodeWithText("Delete schedule").performScrollTo().performClick()
        composeRule.onNodeWithText("Delete").performClick()
        composeRule.waitUntil { composeRule.onAllNodesWithText("Flow test").fetchSemanticsNodes().isEmpty() }
    }

    @Test
    fun cancellingAnEditorWithWorkInItAsksFirst() {
        composeRule.onNodeWithText("Schedules").performClick()
        composeRule.onNodeWithText("New schedule").performClick()

        // An untouched draft is the design's defaults and nobody's work, so Cancel just leaves.
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNodeWithText("New schedule").performClick()

        composeRule.onNode(hasSetTextAction()).performTextInput("Flow test")
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNodeWithText("Discard this schedule?").assertIsDisplayed()

        // Keeping the dialog's promise: the draft is still there, name and all.
        composeRule.onNodeWithText("Keep editing").performClick()
        composeRule.onNodeWithText("Flow test").assertIsDisplayed()

        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNodeWithText("Discard").performClick()
        composeRule.waitUntil { composeRule.onAllNodesWithText("RHYTHM").fetchSemanticsNodes().isEmpty() }
        // Discarded rather than saved: nothing by that name reached the list.
        composeRule.onNodeWithText("Flow test").assertDoesNotExist()
    }

    @Test
    fun settingsShowsThePerDeviceCountdownSwitch() {
        composeRule.onNodeWithText("Settings").performClick()

        // The setting that is per device and never syncs — the distinction the whole product rests
        // on once four devices share one account.
        composeRule.onNodeWithText("Show countdown in notification").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("PERMISSIONS").performScrollTo().assertIsDisplayed()

        // The switch itself, not the shape that stands in for it while the preferences file is
        // being read: the screen is built fresh on every visit, and this is what proves it is built
        // from the stored value rather than from the defaults.
        composeRule.onNode(isToggleable()).assertIsOff()
    }
}
