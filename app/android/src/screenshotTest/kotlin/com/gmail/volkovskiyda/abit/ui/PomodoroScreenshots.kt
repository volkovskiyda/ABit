package com.gmail.volkovskiyda.abit.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.gmail.volkovskiyda.abit.core.datastore.ThemeMode
import com.gmail.volkovskiyda.abit.core.domain.SyncState
import com.gmail.volkovskiyda.abit.feature.pomodoro.impl.PomodoroUiState
import com.gmail.volkovskiyda.abit.ui.theme.AbitTheme

/**
 * Golden images of the pomodoro screen. Each `@PreviewTest` is rendered by LayoutLib at build time
 * and diffed against a committed PNG, which catches the kind of regression a semantics assertion
 * cannot see: a clipped label, a broken dark palette, text that stops fitting at a larger font.
 *
 * Re-bake with `./gradlew :app:android:updateDebugScreenshotTest` after a deliberate change, then
 * look at the diff before committing it. The goldens are LFS objects — see `.gitattributes`.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
private fun PomodoroSignedOutLight() {
    AbitTheme(themeMode = ThemeMode.Light) {
        PomodoroContent(PomodoroUiState())
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
private fun PomodoroSignedOutDark() {
    AbitTheme(themeMode = ThemeMode.Dark) {
        PomodoroContent(PomodoroUiState())
    }
}

/** The largest font scale the system offers, where a cramped layout gives way first. */
@PreviewTest
@Preview(showBackground = true, fontScale = 2.0f)
@Composable
private fun PomodoroLargestFont() {
    AbitTheme(themeMode = ThemeMode.Light) {
        PomodoroContent(PomodoroUiState(syncState = SyncState.Syncing))
    }
}
