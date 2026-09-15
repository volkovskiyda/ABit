package com.gmail.volkovskiyda.abit

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.isTraySupported
import androidx.compose.ui.window.rememberTrayState
import androidx.compose.ui.window.rememberWindowState
import com.gmail.volkovskiyda.abit.app.shared.initKoin
import com.gmail.volkovskiyda.abit.app.shared.startChimes
import com.gmail.volkovskiyda.abit.app.shared.startSync
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
import com.gmail.volkovskiyda.abit.ui.SchedulesWindowContent
import com.gmail.volkovskiyda.abit.ui.TrayPopoverContent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener

private val POPOVER_WIDTH = 320.dp
private val POPOVER_HEIGHT = 440.dp

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
    // Before `application`, not inside it: the composition can be recreated, and starting Koin
    // twice throws.
    initKoin().startSync().startChimes()

    application {
        var popoverVisible by remember { mutableStateOf(!isTraySupported) }
        var schedulesVisible by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        val preferences: UserPreferencesRepository = koinInject()
        val themeFlow = remember(preferences) { preferences.preferences.map { it.themeMode } }
        val themeMode by themeFlow.collectAsState(initial = ThemeMode.System)
        val chimeFlow = remember(preferences) { preferences.preferences.map { it.chimeOnThisDevice } }
        val chimeOnThisMac by chimeFlow.collectAsState(initial = true)

        AbitMenuBarItem(
            onOpen = { popoverVisible = true },
            onOpenSchedules = { schedulesVisible = true },
            onQuit = ::exitApplication,
        )

        AbitPopoverWindow(
            visible = popoverVisible,
            themeMode = themeMode,
            chimeOnThisMac = chimeOnThisMac,
            onDismiss = { popoverVisible = false },
            onChimeOnThisMac = { enabled ->
                scope.launch { preferences.update { it.copy(chimeOnThisDevice = enabled) } }
            },
            onOpenSchedules = { schedulesVisible = true },
            onQuit = ::exitApplication,
        )

        if (schedulesVisible) {
            Window(
                onCloseRequest = { schedulesVisible = false },
                title = "ABit — Schedules",
                state = rememberWindowState(width = 900.dp, height = 640.dp),
            ) {
                AbitTheme(darkTheme = themeMode == ThemeMode.Dark) {
                    val viewModel: SchedulesViewModel = koinViewModel()
                    val state by viewModel.state.collectAsState()
                    SchedulesWindowContent(state = state, onToggle = viewModel::toggle)
                }
            }
        }
    }
}

/**
 * The menu-bar item itself. Split out of `main` because it is the one part with real logic: it
 * observes the countdown the scheduler publishes whether or not anything is on screen — which is the
 * point, since the menu bar is the surface that is always visible.
 */
@Composable
private fun ApplicationScope.AbitMenuBarItem(
    onOpen: () -> Unit,
    onOpenSchedules: () -> Unit,
    onQuit: () -> Unit,
) {
    // A headless CI runner and some Linux desktops have no system tray. Falling back to a plain
    // visible window keeps `:app:desktop:run` usable there instead of starting a process with no way
    // to reach it.
    if (!isTraySupported) return

    val trayState = rememberTrayState()
    val scheduler: ChimeScheduler = koinInject()
    val polling = scheduler as? PollingChimeScheduler
    val minutes by (polling?.minutesLeft ?: remember { MutableStateFlow(null) })
        .collectAsState(initial = null)
    val today by (polling?.todayState ?: remember { MutableStateFlow(null) })
        .collectAsState(initial = null)
    val mode = (today as? TodayState.Running)?.stage

    Tray(
        icon = remember(minutes, mode) { AbitTrayPainter(minutes, mode) },
        state = trayState,
        tooltip = "ABit",
        onAction = onOpen,
        menu = {
            Item("Open ABit", onClick = onOpen)
            Item("Schedules…", onClick = onOpenSchedules)
            Separator()
            Item("Quit ABit", onClick = onQuit)
        },
    )
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
    themeMode: ThemeMode,
    chimeOnThisMac: Boolean,
    onDismiss: () -> Unit,
    onChimeOnThisMac: (Boolean) -> Unit,
    onOpenSchedules: () -> Unit,
    onQuit: () -> Unit,
) {
    Window(
        visible = visible,
        onCloseRequest = onDismiss,
        title = "ABit",
        undecorated = true,
        transparent = true,
        alwaysOnTop = true,
        resizable = false,
        state =
            rememberWindowState(
                width = POPOVER_WIDTH,
                height = POPOVER_HEIGHT,
                position = WindowPosition(Alignment.TopEnd),
            ),
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
            val viewModel: TodayViewModel = koinViewModel()
            val state by viewModel.state.collectAsState()
            TrayPopoverContent(
                state = state,
                chimeOnThisMac = chimeOnThisMac,
                onPauseToday = { viewModel.pauseToday(it) },
                onSkipNext = viewModel::skipNext,
                onChimeOnThisMac = onChimeOnThisMac,
                onOpenSchedules = onOpenSchedules,
                onQuit = onQuit,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
