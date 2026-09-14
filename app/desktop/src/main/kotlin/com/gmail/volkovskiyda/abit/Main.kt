package com.gmail.volkovskiyda.abit

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.isTraySupported
import androidx.compose.ui.window.rememberTrayState
import androidx.compose.ui.window.rememberWindowState
import com.gmail.volkovskiyda.abit.app.shared.initKoin
import com.gmail.volkovskiyda.abit.ui.AbitTheme
import com.gmail.volkovskiyda.abit.ui.PomodoroPopup
import com.gmail.volkovskiyda.abit.ui.TrayIcon
import org.koin.compose.viewmodel.koinViewModel

/**
 * A menu-bar app, not a windowed one: the bundle sets `LSUIElement` (see build.gradle.kts) so macOS
 * gives it no Dock icon and no menu bar of its own. The tray icon is the whole entry point, and the
 * window is a popup it toggles.
 */
fun main() {
    // Before `application`, not inside it: the composition can be recreated, and starting Koin
    // twice throws.
    initKoin()

    application {
        var popupVisible by remember { mutableStateOf(!isTraySupported) }
        val trayState = rememberTrayState()

        // A headless CI runner and some Linux desktops have no system tray. Falling back to a
        // plain visible window keeps `:app:desktop:run` usable there instead of starting a process
        // with no way to reach it.
        if (isTraySupported) {
            Tray(
                icon = TrayIcon,
                state = trayState,
                tooltip = "ABit",
                onAction = { popupVisible = true },
                menu = {
                    Item("Open ABit", onClick = { popupVisible = true })
                    Separator()
                    Item("Quit ABit", onClick = ::exitApplication)
                },
            )
        }

        Window(
            visible = popupVisible,
            onCloseRequest = { popupVisible = false },
            title = "ABit",
            state = rememberWindowState(width = 360.dp, height = 480.dp),
            alwaysOnTop = true,
            resizable = false,
        ) {
            AbitTheme {
                PomodoroPopup(viewModel = koinViewModel())
            }
        }
    }
}
