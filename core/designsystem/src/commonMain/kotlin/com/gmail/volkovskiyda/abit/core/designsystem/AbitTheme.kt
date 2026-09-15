package com.gmail.volkovskiyda.abit.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

/** Whether the current theme is the dark one, for the two arc colours that are not Material roles. */
val LocalAbitDarkTheme = staticCompositionLocalOf { false }

private val abitShapes =
    Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(24.dp),
    )

/**
 * The one theme every ABit surface uses, phone, tablet, web and desktop. Wear has its own Material 3
 * and builds its scheme from [AbitTokens] directly.
 *
 * **No dynamic colour.** The palette carries meaning — tangerine is Focus, mint is Break — and a
 * device-tinted scheme would repaint that meaning at random, leaving a Focus ring in whatever hue
 * the user's wallpaper happened to be.
 */
@Composable
fun AbitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalAbitDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) abitDarkColorScheme() else abitLightColorScheme(),
            typography = abitTypography(),
            shapes = abitShapes,
            content = content,
        )
    }
}
