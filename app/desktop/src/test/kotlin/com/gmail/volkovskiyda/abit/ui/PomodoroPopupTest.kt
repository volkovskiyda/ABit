package com.gmail.volkovskiyda.abit.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.gmail.volkovskiyda.abit.core.domain.SyncState
import com.gmail.volkovskiyda.abit.feature.pomodoro.impl.PomodoroUiState
import kotlin.test.Test

/**
 * The desktop popup, rendered in-process through Skiko — no emulator, no display server, so it runs
 * anywhere the JVM does. It exercises the stateless content, which is why it needs no Koin graph.
 */
@OptIn(ExperimentalTestApi::class)
class PomodoroPopupTest {
    @Test
    fun `shows the session count and sync state`() =
        runComposeUiTest {
            setContent {
                PomodoroPopupContent(PomodoroUiState(syncState = SyncState.Unavailable))
            }

            onNodeWithText("ABit").assertIsDisplayed()
            onNodeWithText("Sessions: 0").assertIsDisplayed()
            // The wording matters: a build without Firebase is not broken, and the UI has to say so.
            onNodeWithText("Sync unavailable in this build").assertIsDisplayed()
        }

    @Test
    fun `offers an account-free start when signed out`() =
        runComposeUiTest {
            setContent { PomodoroPopupContent(PomodoroUiState()) }

            onNodeWithText("Use without an account").assertIsDisplayed()
        }
}
