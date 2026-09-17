package com.gmail.volkovskiyda.abit.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.gmail.volkovskiyda.abit.core.designsystem.AbitTheme
import com.gmail.volkovskiyda.abit.core.domain.AuthUser
import com.gmail.volkovskiyda.abit.core.domain.Block
import com.gmail.volkovskiyda.abit.core.domain.BlockKind
import com.gmail.volkovskiyda.abit.core.domain.DayPlan
import com.gmail.volkovskiyda.abit.core.domain.Session
import com.gmail.volkovskiyda.abit.core.domain.TodayState
import com.gmail.volkovskiyda.abit.core.model.UserId
import com.gmail.volkovskiyda.abit.feature.today.impl.TodayUiState
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * The menu-bar popover, rendered in-process through Skiko — no display server, so it runs anywhere
 * the JVM does. It exercises the stateless content, which is why it needs no Koin graph.
 */
@OptIn(ExperimentalTestApi::class)
class TrayPopoverTest {
    @Test
    fun `shows the countdown and the two interventions`() =
        runComposeUiTest {
            setContent {
                AbitTheme(darkTheme = false) {
                    TrayPopoverContent(
                        state = TodayUiState(today = running()),
                        chimeOnThisMac = true,
                        onPauseToday = {},
                        onSkipNext = {},
                        onChimeOnThisMac = {},
                        onOpenSchedules = {},
                        onQuit = {},
                    )
                }
            }

            onNodeWithText("FOCUS").assertIsDisplayed()
            onNodeWithText("37:38").assertIsDisplayed()
            onNodeWithText("Pause today").assertIsDisplayed()
            onNodeWithText("Skip next").assertIsDisplayed()
        }

    @Test
    fun `has no start control, and offers the Mac's own chime switch`() =
        runComposeUiTest {
            setContent {
                AbitTheme(darkTheme = false) {
                    TrayPopoverContent(
                        state = TodayUiState(today = running()),
                        chimeOnThisMac = true,
                        onPauseToday = {},
                        onSkipNext = {},
                        onChimeOnThisMac = {},
                        onOpenSchedules = {},
                        onQuit = {},
                    )
                }
            }

            onNodeWithText("Chime on this Mac").assertIsDisplayed()
            onNodeWithText("Schedules…").assertIsDisplayed()
            onNodeWithText("Start").assertDoesNotExist()
        }

    /**
     * The account row, in the state a fresh clone is in: no `oauth.properties`, so there is nothing
     * to ask Google for and the row says so rather than offering a button that cannot work.
     */
    @Test
    fun `says sign-in is unavailable when the build has no OAuth client`() =
        runComposeUiTest {
            setContent {
                AbitTheme(darkTheme = false) {
                    TrayPopoverContent(
                        state = TodayUiState(today = running(), user = anonymous()),
                        chimeOnThisMac = true,
                        onPauseToday = {},
                        onSkipNext = {},
                        onChimeOnThisMac = {},
                        onOpenSchedules = {},
                        onQuit = {},
                        signInAvailable = false,
                    )
                }
            }

            onNodeWithText("Not syncing").assertIsDisplayed()
            onNodeWithText("Sign-in unavailable").assertIsDisplayed()
            onNodeWithText("Sign in…").assertDoesNotExist()
        }

    /** Anonymous is not signed in: Firebase has issued a uid, but nothing leaves this Mac yet. */
    @Test
    fun `offers sign-in to an anonymous user when the build has a client`() =
        runComposeUiTest {
            setContent {
                AbitTheme(darkTheme = false) {
                    TrayPopoverContent(
                        state = TodayUiState(today = running(), user = anonymous()),
                        chimeOnThisMac = true,
                        onPauseToday = {},
                        onSkipNext = {},
                        onChimeOnThisMac = {},
                        onOpenSchedules = {},
                        onQuit = {},
                        signInAvailable = true,
                    )
                }
            }

            onNodeWithText("Not syncing").assertIsDisplayed()
            onNodeWithText("Sign in…").assertIsDisplayed()
        }

    @Test
    fun `names the signed-in account and offers sign-out`() =
        runComposeUiTest {
            setContent {
                AbitTheme(darkTheme = false) {
                    TrayPopoverContent(
                        state =
                            TodayUiState(
                                today = running(),
                                user =
                                    AuthUser(
                                        id = UserId("uid"),
                                        isAnonymous = false,
                                        email = "someone@example.com",
                                    ),
                            ),
                        chimeOnThisMac = true,
                        onPauseToday = {},
                        onSkipNext = {},
                        onChimeOnThisMac = {},
                        onOpenSchedules = {},
                        onQuit = {},
                        signInAvailable = true,
                    )
                }
            }

            onNodeWithText("someone@example.com").assertIsDisplayed()
            onNodeWithText("Sign out").assertIsDisplayed()
        }

    /** A failed attempt replaces the account line rather than opening anything. */
    @Test
    fun `shows a sign-in failure in the account row`() =
        runComposeUiTest {
            setContent {
                AbitTheme(darkTheme = false) {
                    TrayPopoverContent(
                        state = TodayUiState(today = running(), user = anonymous(), authError = "Sign-in cancelled"),
                        chimeOnThisMac = true,
                        onPauseToday = {},
                        onSkipNext = {},
                        onChimeOnThisMac = {},
                        onOpenSchedules = {},
                        onQuit = {},
                        signInAvailable = true,
                    )
                }
            }

            onNodeWithText("Sign-in cancelled").assertIsDisplayed()
            onNodeWithText("Not syncing").assertDoesNotExist()
        }

    private fun anonymous() = AuthUser(id = UserId("anon"), isAnonymous = true)

    @Test
    fun `the tray image stays square whether or not it carries minutes`() {
        // Compose's Tray calls setImageAutoSize(true), which scales a non-square image to the menu
        // bar's square. Keeping the intrinsic size square is what stops the digits being squashed.
        val withMinutes = AbitTrayPainter(minutes = 38, mode = BlockKind.Focus).intrinsicSize
        val without = AbitTrayPainter(minutes = null, mode = null).intrinsicSize

        assertEquals(withMinutes, without)
        assertTrue(withMinutes.width == withMinutes.height)
    }

    private fun running(): TodayState.Running {
        val focus = Block(BlockKind.Focus, LocalTime(9, 0), LocalTime(9, 45))
        val rest = Block(BlockKind.Break, LocalTime(9, 45), LocalTime(10, 0))
        val session = Session(index = 0, focus = focus, rest = rest)
        return TodayState.Running(
            plan = DayPlan(LocalDate(2026, 9, 14), schedule = null, sessions = listOf(session)),
            session = session,
            sessionNumber = 1,
            sessionCount = 9,
            stage = BlockKind.Focus,
            remaining = 37.minutes + 38.seconds,
            nextBoundary = LocalTime(9, 45),
        )
    }
}
