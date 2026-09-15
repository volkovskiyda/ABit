package com.gmail.volkovskiyda.abit.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private fun Long.color() = Color(this)

fun abitLightColorScheme(): ColorScheme =
    with(AbitTokens.Light) {
        lightColorScheme(
            primary = PRIMARY.color(),
            onPrimary = ON_PRIMARY.color(),
            primaryContainer = PRIMARY_CONTAINER.color(),
            onPrimaryContainer = ON_PRIMARY_CONTAINER.color(),
            inversePrimary = INVERSE_PRIMARY.color(),
            secondary = SECONDARY.color(),
            onSecondary = ON_SECONDARY.color(),
            secondaryContainer = SECONDARY_CONTAINER.color(),
            onSecondaryContainer = ON_SECONDARY_CONTAINER.color(),
            tertiary = TERTIARY.color(),
            onTertiary = ON_TERTIARY.color(),
            tertiaryContainer = TERTIARY_CONTAINER.color(),
            onTertiaryContainer = ON_TERTIARY_CONTAINER.color(),
            error = ERROR.color(),
            onError = ON_ERROR.color(),
            errorContainer = ERROR_CONTAINER.color(),
            onErrorContainer = ON_ERROR_CONTAINER.color(),
            background = SURFACE.color(),
            onBackground = ON_SURFACE.color(),
            surface = SURFACE.color(),
            onSurface = ON_SURFACE.color(),
            surfaceVariant = SURFACE_VARIANT.color(),
            onSurfaceVariant = ON_SURFACE_VARIANT.color(),
            surfaceTint = PRIMARY.color(),
            inverseSurface = INVERSE_SURFACE.color(),
            inverseOnSurface = INVERSE_ON_SURFACE.color(),
            outline = OUTLINE.color(),
            outlineVariant = OUTLINE_VARIANT.color(),
            surfaceBright = SURFACE_BRIGHT.color(),
            surfaceDim = SURFACE_DIM.color(),
            surfaceContainer = SURFACE_CONTAINER.color(),
            surfaceContainerHigh = SURFACE_CONTAINER_HIGH.color(),
            surfaceContainerHighest = SURFACE_CONTAINER_HIGHEST.color(),
            surfaceContainerLow = SURFACE_CONTAINER_LOW.color(),
            surfaceContainerLowest = SURFACE_CONTAINER_LOWEST.color(),
        )
    }

fun abitDarkColorScheme(): ColorScheme =
    with(AbitTokens.Dark) {
        darkColorScheme(
            primary = PRIMARY.color(),
            onPrimary = ON_PRIMARY.color(),
            primaryContainer = PRIMARY_CONTAINER.color(),
            onPrimaryContainer = ON_PRIMARY_CONTAINER.color(),
            inversePrimary = INVERSE_PRIMARY.color(),
            secondary = SECONDARY.color(),
            onSecondary = ON_SECONDARY.color(),
            secondaryContainer = SECONDARY_CONTAINER.color(),
            onSecondaryContainer = ON_SECONDARY_CONTAINER.color(),
            tertiary = TERTIARY.color(),
            onTertiary = ON_TERTIARY.color(),
            tertiaryContainer = TERTIARY_CONTAINER.color(),
            onTertiaryContainer = ON_TERTIARY_CONTAINER.color(),
            error = ERROR.color(),
            onError = ON_ERROR.color(),
            errorContainer = ERROR_CONTAINER.color(),
            onErrorContainer = ON_ERROR_CONTAINER.color(),
            background = SURFACE.color(),
            onBackground = ON_SURFACE.color(),
            surface = SURFACE.color(),
            onSurface = ON_SURFACE.color(),
            surfaceVariant = SURFACE_VARIANT.color(),
            onSurfaceVariant = ON_SURFACE_VARIANT.color(),
            surfaceTint = PRIMARY.color(),
            inverseSurface = INVERSE_SURFACE.color(),
            inverseOnSurface = INVERSE_ON_SURFACE.color(),
            outline = OUTLINE.color(),
            outlineVariant = OUTLINE_VARIANT.color(),
            surfaceBright = SURFACE_BRIGHT.color(),
            surfaceDim = SURFACE_DIM.color(),
            surfaceContainer = SURFACE_CONTAINER.color(),
            surfaceContainerHigh = SURFACE_CONTAINER_HIGH.color(),
            surfaceContainerHighest = SURFACE_CONTAINER_HIGHEST.color(),
            surfaceContainerLow = SURFACE_CONTAINER_LOW.color(),
            surfaceContainerLowest = SURFACE_CONTAINER_LOWEST.color(),
        )
    }

/**
 * The ring's three remaining colours, read from the tokens rather than from a `ColorScheme`.
 *
 * On phone, tablet, desktop and web these are exactly what [abitLightColorScheme] and
 * [abitDarkColorScheme] map the same tokens to, so nothing moves. **On Wear they are the difference
 * between the ring being right and being wrong**: `core:designsystem`'s components are shared, but
 * Wear runs its own Material 3, so a composable that reaches for the multiplatform
 * `MaterialTheme.colorScheme` on a watch is served Material's *baseline* palette — a pale lilac for
 * `primaryContainer` and a pale pink for `tertiaryContainer` — because nothing on that screen ever
 * provided the multiplatform theme. That is not a colour a watch could ever have shown on purpose.
 */
fun ringTrackColor(darkTheme: Boolean): Color =
    if (darkTheme) AbitTokens.Dark.SURFACE_CONTAINER_HIGHEST.color() else AbitTokens.Light.SURFACE_CONTAINER_HIGHEST.color()

/** The break arc when the focus is the stage that is running: mint, dimmed to a container tone. */
fun breakMutedArcColor(darkTheme: Boolean): Color =
    if (darkTheme) AbitTokens.Dark.TERTIARY_CONTAINER.color() else AbitTokens.Light.TERTIARY_CONTAINER.color()

/** The focus arc once the break is the stage that is running. */
fun focusMutedArcColor(darkTheme: Boolean): Color =
    if (darkTheme) AbitTokens.Dark.PRIMARY_CONTAINER.color() else AbitTokens.Light.PRIMARY_CONTAINER.color()

/** The break arc's colour, which is not a Material role: light needs a brighter mint than its text. */
fun breakArcColor(darkTheme: Boolean): Color =
    if (darkTheme) AbitTokens.Dark.TERTIARY_ARC.color() else AbitTokens.Light.TERTIARY_ARC.color()

/** The focus arc's colour: the raw tangerine in both themes, which is why it is not `primary`. */
fun focusArcColor(darkTheme: Boolean): Color = if (darkTheme) AbitTokens.Dark.PRIMARY.color() else AbitTokens.Light.INVERSE_PRIMARY.color()
