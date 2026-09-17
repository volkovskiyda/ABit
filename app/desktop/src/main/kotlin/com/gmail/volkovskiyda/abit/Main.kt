package com.gmail.volkovskiyda.abit

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.isTraySupported
import androidx.compose.ui.window.rememberWindowState
import com.gmail.volkovskiyda.abit.app.shared.initKoin
import com.gmail.volkovskiyda.abit.app.shared.startChimes
import com.gmail.volkovskiyda.abit.app.shared.startSync
import com.gmail.volkovskiyda.abit.auth.GoogleSignIn
import com.gmail.volkovskiyda.abit.core.chime.ChimeScheduler
import com.gmail.volkovskiyda.abit.core.chime.PollingChimeScheduler
import com.gmail.volkovskiyda.abit.core.datastore.ThemeMode
import com.gmail.volkovskiyda.abit.core.datastore.UserPreferencesRepository
import com.gmail.volkovskiyda.abit.core.designsystem.AbitTheme
import com.gmail.volkovskiyda.abit.core.domain.BlockKind
import com.gmail.volkovskiyda.abit.core.domain.TodayState
import com.gmail.volkovskiyda.abit.feature.schedules.impl.SchedulesViewModel
import com.gmail.volkovskiyda.abit.feature.today.impl.TodayViewModel
import com.gmail.volkovskiyda.abit.ui.AbitTrayPainter
import com.gmail.volkovskiyda.abit.ui.SchedulesPaneContent
import com.gmail.volkovskiyda.abit.ui.TrayPopoverContent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

private const val TRAY_TOOLTIP = "ABit"

/**
 * How recently the popover must have dismissed itself for a menu-bar click to count as the cause.
 *
 * Long enough to cover the focus-loss that a click delivers before the click itself arrives, short
 * enough that two deliberate clicks still read as open-then-close.
 */
private val POPOVER_CLICK_GRACE = 300.milliseconds
private val POPOVER_WIDTH = 320.dp
private val POPOVER_HEIGHT = 440.dp

/**
 * How tall the same popover stands while it is showing the schedules.
 *
 * "Schedules" is a pane, not a window: an accessory app that opens a real window has to fight macOS
 * for the foreground to be seen at all, and the window it wins reads as a second app rather than as
 * the menu-bar item's own surface. Growing downwards from the item keeps one thing on screen, and
 * the width never changes because the item it hangs from does not move.
 */
private val SCHEDULES_HEIGHT = 560.dp

// `collectAsState`, not `collectAsStateWithLifecycle`: on desktop `LocalLifecycleOwner` is provided
// inside a `Window`, and the menu-bar item lives in the `application` scope outside every window.
// The lifecycle-aware variant throws there — found by running the app, not by a test.

/**
 * A menu-bar app, not a windowed one: the bundle sets `LSUIElement` (see build.gradle.kts) so macOS
 * gives it no Dock icon and no menu bar of its own. The menu-bar item is the whole entry point, and it
 * shows the minutes left — drawn into its image, because AWT gives an item no title (see
 * [AbitTrayPainter]).
 */
fun main() {
    // Before any tray icon exists, because AWT reads this once, when it first loads the class that
    // puts one in the menu bar. It makes the item a *template* image: macOS then tints it for the
    // menu bar it is in — white on a dark one, black on a light one — and keeps doing so when the
    // user switches. Without it the dial is drawn in whatever colour this app picked, which on a
    // dark menu bar was black on near-black. See [AbitTrayPainter].
    System.setProperty("apple.awt.enableTemplateImages", "true")

    // Before `application`, not inside it: the composition can be recreated, and starting Koin
    // twice throws.
    initKoin().startSync().startChimes()

    application {
        var popoverVisible by remember { mutableStateOf(!isTraySupported) }
        var popoverDismissed by remember { mutableStateOf<TimeMark?>(null) }
        var schedulesPane by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        val preferences: UserPreferencesRepository = koinInject()
        val themeFlow = remember(preferences) { preferences.preferences.map { it.themeMode } }
        val themeMode by themeFlow.collectAsState(initial = ThemeMode.System)
        val chimeFlow = remember(preferences) { preferences.preferences.map { it.chimeOnThisDevice } }
        val chimeOnThisMac by chimeFlow.collectAsState(initial = true)

        AbitMenuBarItem(
            // The item stays lit for as long as the popover it opened is up, which is what a menu
            // bar extra does while its menu is open.
            highlighted = popoverVisible,
            onClick = {
                // Clicking the item takes focus off the popover, which dismisses itself when that
                // happens — so by the time this runs, "is it open?" can already read false for the
                // very click meant to close it, and a plain toggle would reopen what the user just
                // closed. A dismissal this recent *is* this click. The other order, where the click
                // arrives first, closes it here instead; both end shut.
                val closedByThisClick = popoverDismissed?.elapsedNow()?.let { it < POPOVER_CLICK_GRACE } == true
                popoverVisible = !popoverVisible && !closedByThisClick
            },
        )

        AbitPopoverWindow(
            visible = popoverVisible,
            schedulesPane = schedulesPane,
            themeMode = themeMode,
            chimeOnThisMac = chimeOnThisMac,
            onDismiss = {
                popoverVisible = false
                popoverDismissed = TimeSource.Monotonic.markNow()
                // A popover that reopened where it was left would come back at whatever size the
                // last visit ended on; the menu-bar item's own surface is Today.
                schedulesPane = false
            },
            onChimeOnThisMac = { enabled ->
                scope.launch { preferences.update { it.copy(chimeOnThisDevice = enabled) } }
            },
            onOpenSchedules = { schedulesPane = true },
            onCloseSchedules = { schedulesPane = false },
            onQuit = ::exitApplication,
        )
    }
}

/**
 * The menu-bar item itself. Split out of `main` because it is the one part with real logic: it
 * observes the countdown the scheduler publishes whether or not anything is on screen — which is the
 * point, since the menu bar is the surface that is always visible.
 *
 * Raw `java.awt.SystemTray` rather than Compose's `Tray`, which [AbitTrayPainter] already names as
 * the fallback, and for a reason that is macOS-specific and absolute: `Tray` assigns its
 * `PopupMenu` to the icon unconditionally, and on macOS an AWT tray icon with a popup menu hands
 * *every* left click to that menu. Its `onAction` is documented as a right click here. So no
 * arrangement of that composable can make one click open the app — a click could only ever open a
 * menu whose first item opened the app. Owning the `TrayIcon` means owning its mouse events.
 *
 * There is no menu at all now, and nothing is lost with it: the popover carries "Schedules" and
 * "Quit", which is where the menu's two other items led. `mousePressed` rather than `mouseClicked`
 * is what a menu-bar item does natively — it opens on the way down, not on release. What the click
 * *means* is [main]'s to decide, because only it knows whether the popover is up.
 */
@Composable
private fun ApplicationScope.AbitMenuBarItem(
    highlighted: Boolean,
    onClick: () -> Unit,
) {
    // A headless CI runner and some Linux desktops have no system tray. Falling back to a plain
    // visible window keeps `:app:desktop:run` usable there instead of starting a process with no way
    // to reach it.
    if (!isTraySupported) return

    val scheduler: ChimeScheduler = koinInject()
    val polling = scheduler as? PollingChimeScheduler
    val minutes by (polling?.minutesLeft ?: remember { MutableStateFlow(null) })
        .collectAsState(initial = null)
    val today by (polling?.todayState ?: remember { MutableStateFlow(null) })
        .collectAsState(initial = null)
    val mode = (today as? TodayState.Running)?.stage

    val painter = remember(minutes, mode, highlighted) { AbitTrayPainter(minutes, mode, highlighted = highlighted) }
    // Auto-size off, because it is the setting that forces the image into a square — macOS scales
    // by height and keeps the aspect without it, which is what gives the item its padding. See
    // [AbitTrayPainter], which draws at twice the menu bar's height for the same reason.
    val image = remember(painter) { painter.toAwtImage(Density(1f), LayoutDirection.Ltr, painter.intrinsicSize) }
    val trayIcon = remember { TrayIcon(image).apply { isImageAutoSize = false } }
    val currentOnClick by rememberUpdatedState(onClick)

    SideEffect {
        if (trayIcon.image !== image) trayIcon.image = image
        if (trayIcon.toolTip != TRAY_TOOLTIP) trayIcon.toolTip = TRAY_TOOLTIP
    }

    DisposableEffect(Unit) {
        val listener =
            object : MouseAdapter() {
                override fun mousePressed(event: MouseEvent?) = currentOnClick()
            }
        trayIcon.addMouseListener(listener)
        SystemTray.getSystemTray().add(trayIcon)
        onDispose {
            trayIcon.removeMouseListener(listener)
            SystemTray.getSystemTray().remove(trayIcon)
        }
    }
}

/**
 * The popover. `undecorated` plus `transparent` is what makes it read as a popover rather than a
 * small window, and dismiss-on-focus-loss is what finishes the illusion — Compose has no popover
 * primitive on desktop, so that last part goes through AWT.
 */
@Suppress("LongParameterList")
@Composable
private fun AbitPopoverWindow(
    visible: Boolean,
    schedulesPane: Boolean,
    themeMode: ThemeMode,
    chimeOnThisMac: Boolean,
    onDismiss: () -> Unit,
    onChimeOnThisMac: (Boolean) -> Unit,
    onOpenSchedules: () -> Unit,
    onCloseSchedules: () -> Unit,
    onQuit: () -> Unit,
) {
    val state =
        rememberWindowState(
            width = POPOVER_WIDTH,
            height = POPOVER_HEIGHT,
            position = WindowPosition(Alignment.TopEnd),
        )
    // The pane the popover is showing is what decides its height, and the window follows: the
    // schedules arrive by the popover growing downwards, not by a second window opening. The width
    // is left alone on purpose — the popover hangs off a menu-bar item, and an edge that moved
    // would unhook it from the thing it belongs to. `resizable = false` bars the *user* from
    // dragging an edge; it does not bar this.
    LaunchedEffect(schedulesPane) {
        state.size = DpSize(POPOVER_WIDTH, if (schedulesPane) SCHEDULES_HEIGHT else POPOVER_HEIGHT)
    }

    Window(
        visible = visible,
        onCloseRequest = onDismiss,
        title = "ABit",
        undecorated = true,
        transparent = true,
        alwaysOnTop = true,
        resizable = false,
        state = state,
    ) {
        LaunchedEffect(window) {
            val listener =
                object : WindowFocusListener {
                    override fun windowGainedFocus(event: WindowEvent?) = Unit

                    override fun windowLostFocus(event: WindowEvent?) = onDismiss()
                }
            window.addWindowFocusListener(listener)
        }

        AbitTheme(darkTheme = themeMode == ThemeMode.Dark) {
            if (schedulesPane) {
                SchedulesPane(onBack = onCloseSchedules)
            } else {
                TodayPane(
                    chimeOnThisMac = chimeOnThisMac,
                    onChimeOnThisMac = onChimeOnThisMac,
                    onOpenSchedules = onOpenSchedules,
                    onQuit = onQuit,
                )
            }
        }
    }
}

/** The popover's first pane, wired to the ViewModel the window keeps for as long as it lives. */
@Composable
private fun TodayPane(
    chimeOnThisMac: Boolean,
    onChimeOnThisMac: (Boolean) -> Unit,
    onOpenSchedules: () -> Unit,
    onQuit: () -> Unit,
) {
    val viewModel: TodayViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    // Remembered, not injected: it holds no state worth sharing and its only dependency is
    // a resource the build put in the jar.
    val googleSignIn = remember { GoogleSignIn() }
    val signInScope = rememberCoroutineScope()
    TrayPopoverContent(
        state = state,
        chimeOnThisMac = chimeOnThisMac,
        onSkipToday = { viewModel.skipToday(it) },
        onSkipTomorrow = { viewModel.skipTomorrow() },
        onChimeOnThisMac = onChimeOnThisMac,
        onOpenSchedules = onOpenSchedules,
        onQuit = onQuit,
        modifier = Modifier.fillMaxSize(),
        signInAvailable = googleSignIn.available,
        onSignIn = {
            signInScope.launch {
                googleSignIn
                    .requestIdToken()
                    // The token goes to the ViewModel, which **links** it to the anonymous
                    // account rather than replacing it — so schedules made before signing in
                    // survive and start syncing.
                    .onSuccess(viewModel::signInWithGoogle)
                    .onFailure { viewModel.onAuthError(it.message ?: "Sign-in failed") }
            }
        },
        onSignOut = viewModel::signOut,
    )
}

/**
 * The second pane, in the same window.
 *
 * Its ViewModel outlives the swap back to Today — a `ViewModelStoreOwner` is the window, not the
 * composition — so returning to the schedules does not re-query anything.
 */
@Composable
private fun SchedulesPane(onBack: () -> Unit) {
    val viewModel: SchedulesViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    SchedulesPaneContent(
        state = state,
        onToggle = viewModel::toggle,
        onBack = onBack,
        modifier = Modifier.fillMaxSize(),
    )
}
