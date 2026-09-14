package com.gmail.volkovskiyda.abit.wear

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The watch app starts and renders on a round Wear screen. That is a real check rather than a
 * formality: the Wear app shares the whole object graph with the phone, so anything the phone adds
 * to it — a Context-dependent binding, a database migration — breaks here first.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun rendersOnAWatch() {
        composeRule.onNodeWithText("ABit").assertIsDisplayed()
        composeRule.onNodeWithText("Sessions: 0").assertIsDisplayed()
    }
}
