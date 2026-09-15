package com.gmail.volkovskiyda.abit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.gmail.volkovskiyda.abit.core.datastore.ThemeMode
import com.gmail.volkovskiyda.abit.core.designsystem.AbitTheme as SharedAbitTheme

/**
 * A delegate to `core:designsystem`, which owns the palette, the bundled Inter and the shapes.
 *
 * **No dynamic colour any more.** The palette carries meaning — tangerine is Focus, mint is Break —
 * and a wallpaper-tinted scheme would repaint that meaning at random. The [themeMode] parameter is
 * the user's own choice from Settings, which outranks the system's.
 */
@Composable
fun AbitTheme(
    themeMode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit,
) {
    val dark =
        when (themeMode) {
            ThemeMode.System -> isSystemInDarkTheme()
            ThemeMode.Light -> false
            ThemeMode.Dark -> true
        }
    SharedAbitTheme(darkTheme = dark, content = content)
}
