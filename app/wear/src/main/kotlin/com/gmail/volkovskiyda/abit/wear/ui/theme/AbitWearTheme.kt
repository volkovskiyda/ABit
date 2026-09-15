package com.gmail.volkovskiyda.abit.wear.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import com.gmail.volkovskiyda.abit.core.designsystem.AbitTokens
import com.gmail.volkovskiyda.abit.core.designsystem.LocalAbitDarkTheme

private fun Long.color() = Color(this)

/**
 * The watch's own Material 3, built from the **tokens** rather than from `core:designsystem`'s
 * `AbitTheme` — Wear's `ColorScheme` is a different type from the multiplatform one, and its
 * components are sized for a round screen rather than merely themed differently. That the tokens are
 * plain `Long`s with no Compose dependency is what makes this possible at all.
 *
 * The background is **pure black**: the one place in the product where that is allowed, because an
 * OLED watch face saves power on it and the platform convention expects it.
 */
@Composable
fun AbitWearTheme(content: @Composable () -> Unit) {
    // The watch is always the dark theme, and `core:designsystem`'s shared components ask this
    // rather than a scheme for the colours that are not Material roles. Left at its `false` default
    // the session ring drew a watch in the *light* palette's greens.
    CompositionLocalProvider(LocalAbitDarkTheme provides true) {
        MaterialTheme(
            colorScheme =
                with(AbitTokens.Dark) {
                    ColorScheme(
                        primary = PRIMARY.color(),
                        onPrimary = ON_PRIMARY.color(),
                        primaryContainer = PRIMARY_CONTAINER.color(),
                        onPrimaryContainer = ON_PRIMARY_CONTAINER.color(),
                        secondary = SECONDARY.color(),
                        onSecondary = ON_SECONDARY.color(),
                        secondaryContainer = SECONDARY_CONTAINER.color(),
                        onSecondaryContainer = ON_SECONDARY_CONTAINER.color(),
                        tertiary = TERTIARY.color(),
                        onTertiary = ON_TERTIARY.color(),
                        tertiaryContainer = TERTIARY_CONTAINER.color(),
                        onTertiaryContainer = ON_TERTIARY_CONTAINER.color(),
                        surfaceContainerLow = SURFACE_CONTAINER_LOW.color(),
                        surfaceContainer = SURFACE_CONTAINER.color(),
                        surfaceContainerHigh = SURFACE_CONTAINER_HIGH.color(),
                        onSurface = ON_SURFACE.color(),
                        onSurfaceVariant = ON_SURFACE_VARIANT.color(),
                        outline = OUTLINE.color(),
                        outlineVariant = OUTLINE_VARIANT.color(),
                        background = AbitTokens.WEAR_BACKGROUND.color(),
                        onBackground = ON_SURFACE.color(),
                        error = ERROR.color(),
                        onError = ON_ERROR.color(),
                        errorContainer = ERROR_CONTAINER.color(),
                        onErrorContainer = ON_ERROR_CONTAINER.color(),
                    )
                },
            content = content,
        )
    }
}
