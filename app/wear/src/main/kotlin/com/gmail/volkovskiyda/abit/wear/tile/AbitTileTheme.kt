package com.gmail.volkovskiyda.abit.wear.tile

import androidx.wear.protolayout.material3.ColorScheme
import androidx.wear.protolayout.types.LayoutColor
import androidx.wear.protolayout.types.argb
import com.gmail.volkovskiyda.abit.core.designsystem.AbitTokens

internal fun Long.layoutColor(): LayoutColor = argb

/**
 * The tile's palette, from the same tokens as `AbitWearTheme` and for the same reason the watch app
 * builds its own: ProtoLayout's [ColorScheme] is a third type again, distinct from both Compose
 * `ColorScheme`s, so the tokens rather than a theme are what the two surfaces can share.
 *
 * Always the dark set, on a pure black ground — a tile is only ever drawn over the watch's own
 * background.
 *
 * Passed to `materialScope` with `allowDynamicTheme = false`, which is not a stylistic preference:
 * tangerine means Focus and mint means Break, and a device-tinted scheme would repaint that meaning
 * at random. `AbitTokens`' KDoc states the rule; this is the tile obeying it.
 */
internal val AbitTileColorScheme: ColorScheme =
    with(AbitTokens.Dark) {
        ColorScheme(
            primary = PRIMARY.layoutColor(),
            primaryDim = INVERSE_PRIMARY.layoutColor(),
            primaryContainer = PRIMARY_CONTAINER.layoutColor(),
            onPrimary = ON_PRIMARY.layoutColor(),
            onPrimaryContainer = ON_PRIMARY_CONTAINER.layoutColor(),
            secondary = SECONDARY.layoutColor(),
            secondaryDim = SECONDARY.layoutColor(),
            secondaryContainer = SECONDARY_CONTAINER.layoutColor(),
            onSecondary = ON_SECONDARY.layoutColor(),
            onSecondaryContainer = ON_SECONDARY_CONTAINER.layoutColor(),
            tertiary = TERTIARY.layoutColor(),
            tertiaryDim = TERTIARY.layoutColor(),
            tertiaryContainer = TERTIARY_CONTAINER.layoutColor(),
            onTertiary = ON_TERTIARY.layoutColor(),
            onTertiaryContainer = ON_TERTIARY_CONTAINER.layoutColor(),
            surfaceContainerLow = SURFACE_CONTAINER_LOW.layoutColor(),
            surfaceContainer = SURFACE_CONTAINER.layoutColor(),
            surfaceContainerHigh = SURFACE_CONTAINER_HIGH.layoutColor(),
            onSurface = ON_SURFACE.layoutColor(),
            onSurfaceVariant = ON_SURFACE_VARIANT.layoutColor(),
            outline = OUTLINE.layoutColor(),
            outlineVariant = OUTLINE_VARIANT.layoutColor(),
            background = AbitTokens.WEAR_BACKGROUND.layoutColor(),
            onBackground = ON_SURFACE.layoutColor(),
            error = ERROR.layoutColor(),
            errorDim = ERROR.layoutColor(),
            errorContainer = ERROR_CONTAINER.layoutColor(),
            onError = ON_ERROR.layoutColor(),
            onErrorContainer = ON_ERROR_CONTAINER.layoutColor(),
        )
    }
