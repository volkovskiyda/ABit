package com.gmail.volkovskiyda.abit.web

import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeViewport
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gmail.volkovskiyda.abit.app.shared.initKoin
import com.gmail.volkovskiyda.abit.app.shared.startChimes
import com.gmail.volkovskiyda.abit.app.shared.startSync
import com.gmail.volkovskiyda.abit.core.datastore.ThemeMode
import com.gmail.volkovskiyda.abit.core.datastore.UserPreferencesRepository
import com.gmail.volkovskiyda.abit.core.designsystem.AbitTheme
import com.gmail.volkovskiyda.abit.core.designsystem.components.DialMark
import com.gmail.volkovskiyda.abit.web.ui.WebSchedulesScreen
import com.gmail.volkovskiyda.abit.web.ui.WebSettingsScreen
import com.gmail.volkovskiyda.abit.web.ui.WebTodayScreen
import kotlinx.browser.document
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject

/** The three destinations, the same three the phone and the tablet have. */
private enum class WebDestination(
    val label: String,
) {
    Today("Today"),
    Schedules("Schedules"),
    Settings("Settings"),
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    initKoin().startSync().startChimes()

    ComposeViewport(document.body!!) {
        val preferences: UserPreferencesRepository = koinInject()
        val themeFlow = remember(preferences) { preferences.preferences.map { it.themeMode } }
        val themeMode by themeFlow.collectAsStateWithLifecycle(initialValue = ThemeMode.System)
        AbitTheme(
            darkTheme =
                when (themeMode) {
                    ThemeMode.System -> androidx.compose.foundation.isSystemInDarkTheme()
                    ThemeMode.Light -> false
                    ThemeMode.Dark -> true
                },
        ) {
            AbitWebApp()
        }
    }
}

/**
 * The tablet layout is the web layout — the design says so explicitly — so this is one
 * `NavigationSuiteScaffold` that shows a rail in a wide window and a bottom bar in a narrow one.
 *
 * There is no `NavDisplay` here: Navigation 3's back stack is tied to a platform back gesture the
 * browser does not have, and three destinations plus an editor pane do not need one.
 */
@Composable
private fun AbitWebApp() {
    var destination by remember { mutableStateOf(WebDestination.Today) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            WebDestination.entries.forEach { entry ->
                item(
                    selected = entry == destination,
                    onClick = { destination = entry },
                    icon = { if (entry == WebDestination.Today) DialMark(size = 24.dp) else Text(entry.glyph()) },
                    label = { Text(entry.label) },
                )
            }
        },
    ) {
        when (destination) {
            WebDestination.Today -> WebTodayScreen()
            WebDestination.Schedules -> WebSchedulesScreen()
            WebDestination.Settings -> WebSettingsScreen()
        }
    }
}

private fun WebDestination.glyph(): String =
    when (this) {
        WebDestination.Today -> "◷"
        WebDestination.Schedules -> "▤"
        WebDestination.Settings -> "⚙"
    }
