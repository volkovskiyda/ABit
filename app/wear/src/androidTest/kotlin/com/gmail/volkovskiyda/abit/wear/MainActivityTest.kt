package com.gmail.volkovskiyda.abit.wear

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The watch app starts and renders on a round Wear screen. That is a real check rather than a
 * formality: the Wear app shares the whole object graph with the phone, so anything the phone adds
 * to it — a Context-dependent binding, a database migration — breaks here first.
 *
 * These assume the app opened on the ring, which is true whenever Firebase has not resolved the user
 * to an anonymous account: on the managed emulator, where this suite runs and where there is no
 * Firebase behind it at all, and on a watch already signed in. A `--connected` run against a watch
 * that is signed *out* opens on the schedules list instead and will fail them.
 *
 * Do not reach for a swipe to normalise that. Swipe-dismiss on the start destination is Wear's back
 * gesture at the root, so it finishes the Activity and the next assertion finds no compose hierarchy
 * at all — which is how it failed, intermittently, when these tests tried exactly that.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun rendersTheRingOnAWatch() {
        // Off hours with no schedules is what a fresh install shows, and it is still a real render:
        // theme, arcs and the shared object graph all have to work to get this far.
        composeRule.onNodeWithText("OFF HOURS").assertIsDisplayed()
    }

    @Test
    fun opensTheSchedulesListFromTheRing() {
        // The schedules destination carries the sign-in card and the permission rows, and it spent a
        // while registered in the nav graph with nothing anywhere calling navigate(). The button
        // under the ring is the only way in by hand; it must stay reachable.
        composeRule.onNodeWithText("Schedules").performScrollTo().performClick()
        // That the ring is gone is the whole claim: the button navigated. What the list shows next
        // depends on whether the user resolved — the sign-in card, a permission row, the empty state
        // — and which of those is on screen is not a fact about this button. Asserting one of them
        // made this test pass or fail on how far down a round screen the list had been pushed.
        composeRule.onNodeWithText("OFF HOURS").assertDoesNotExist()
    }

    @Test
    fun hasNoStartControl() {
        // The watch used to show a "Start" button that actually signed in anonymously — in an app
        // whose whole premise is that nothing is started by hand. It must not come back.
        composeRule.onNodeWithText("Start").assertDoesNotExist()
    }

    @Test
    fun hasNoSkipControl() {
        // Skipping the day is a big consequence behind one stray tap on a wrist, so it lives on the
        // screens where it reads as a decision. Removed on purpose; keep it removed.
        composeRule.onNodeWithText("Skip today").assertDoesNotExist()
        composeRule.onNodeWithText("Resume today").assertDoesNotExist()
    }
}
