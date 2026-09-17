package com.gmail.volkovskiyda.abit.ui

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
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
    fun `shows the countdown and the one intervention`() =
        runComposeUiTest {
            setContent {
                AbitTheme(darkTheme = false) {
                    TrayPopoverContent(
                        state = TodayUiState(today = running()),
                        onSkipToday = {},
                        onSkipTomorrow = {},
                        onOpenSchedules = {},
                        onQuit = {},
                    )
                }
            }

            onNodeWithText("FOCUS").assertIsDisplayed()
            // The focus block's own countdown, not the session's 37:38.
            onNodeWithText("22:38").assertIsDisplayed()
            onNodeWithText("Skip today").assertIsDisplayed()
        }

    @Test
    fun `has no start control, and no per-device chime switch`() =
        runComposeUiTest {
            setContent {
                AbitTheme(darkTheme = false) {
                    TrayPopoverContent(
                        state = TodayUiState(today = running()),
                        onSkipToday = {},
                        onSkipTomorrow = {},
                        onOpenSchedules = {},
                        onQuit = {},
                    )
                }
            }

            onNodeWithText("Schedules").assertIsDisplayed()
            onNodeWithText("Start").assertDoesNotExist()
            // Silencing one machine belongs to macOS's notification settings, which work whether
            // the app is running or not. An in-app copy of them was one more thing to keep true.
            onNodeWithText("Chime on this Mac").assertDoesNotExist()
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
                        onSkipToday = {},
                        onSkipTomorrow = {},
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
                        onSkipToday = {},
                        onSkipTomorrow = {},
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
                        onSkipToday = {},
                        onSkipTomorrow = {},
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
                        onSkipToday = {},
                        onSkipTomorrow = {},
                        onOpenSchedules = {},
                        onQuit = {},
                        signInAvailable = true,
                    )
                }
            }

            onNodeWithText("Sign-in cancelled").assertIsDisplayed()
            onNodeWithText("Not syncing").assertDoesNotExist()
        }

    /**
     * The popover is the one root in this project with no scaffold over it, so [PopoverSurface] is
     * the only thing that can provide a content colour — and Material's default is `Color.Black`,
     * which on the dark palette painted the countdown, "Rest of today" and every schedule's name
     * into the navy behind them. Asking the composition for the colour is the honest question: the
     * bug was never in a `Text`, it was in what the `Text`s inherit.
     */
    @Test
    fun `the popover provides a content colour rather than Material's black`() =
        runComposeUiTest {
            var inherited: Color? = null
            var onSurface: Color? = null
            setContent {
                AbitTheme(darkTheme = true) {
                    onSurface = MaterialTheme.colorScheme.onSurface
                    PopoverSurface { inherited = LocalContentColor.current }
                }
            }

            assertEquals(onSurface, inherited, "the popover leaves its labels to inherit a colour it never set")
        }

    private fun anonymous() = AuthUser(id = UserId("anon"), isAnonymous = true)

    @Test
    fun `the tray image keeps one size whether or not it carries minutes`() {
        // Auto-size is off, so macOS scales by height and keeps the aspect — the image's width is
        // the item's width, and it must not change as the minutes come and go, or the menu bar
        // shuffles every time one does.
        val withMinutes = AbitTrayPainter(minutes = 38, mode = BlockKind.Focus).intrinsicSize
        val without = AbitTrayPainter(minutes = null, mode = null).intrinsicSize

        assertEquals(withMinutes, without)
        // Wider than tall, because the padding is what the highlight fills: a selection that stops
        // at the glyph reads as a box drawn round the icon, not as the item being lit.
        assertTrue(withMinutes.width > withMinutes.height, "the item has no padding to light up")
    }

    @Test
    fun `the open popover lights the menu-bar item without swallowing its glyph`() {
        // macOS draws that highlight itself only for a native popup menu, which this app does not
        // use — so the image has to carry it, and a pixel is the only honest way to ask whether it
        // does. The top edge's midpoint is inside the fill but outside its rounded corners.
        val lit = AbitTrayPainter(minutes = 38, mode = BlockKind.Focus, highlighted = true).pixels()
        val plain = AbitTrayPainter(minutes = 38, mode = BlockKind.Focus).pixels()
        val fill = lit[lit.width / 2, 1].alpha

        assertEquals(0f, plain[plain.width / 2, 1].alpha, "the unlit item paints a background it should not")
        assertTrue(fill > 0f, "the lit item does not fill the menu-bar item")
        // Alpha is the whole picture: the item is a template image, so macOS tints what this draws
        // and an opaque fill would render the square as one featureless block with no dial in it.
        assertTrue(fill < 0.5f, "the highlight is opaque enough to swallow the dial: alpha $fill")
        assertTrue(lit.mostOpaque() > fill + 0.3f, "the glyph does not stand out of the highlight")
    }

    /** The painter rasterised at its own size, which is what `SystemTray` is handed. */
    private fun AbitTrayPainter.pixels(): PixelMap {
        val bitmap = ImageBitmap(intrinsicSize.width.toInt(), intrinsicSize.height.toInt())
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap), intrinsicSize) {
            draw(intrinsicSize)
        }
        return bitmap.toPixelMap()
    }

    /** The solidest ink in the image, which is the glyph — a template image is its alpha channel. */
    private fun PixelMap.mostOpaque(): Float = (0 until height).maxOf { y -> (0 until width).maxOf { x -> this[x, y].alpha } }

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
            stageRemaining = 22.minutes + 38.seconds,
            sessionRemaining = 37.minutes + 38.seconds,
            nextBoundary = LocalTime(9, 45),
        )
    }
}
