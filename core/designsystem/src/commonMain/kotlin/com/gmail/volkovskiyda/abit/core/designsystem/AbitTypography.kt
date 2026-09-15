package com.gmail.volkovskiyda.abit.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.gmail.volkovskiyda.abit.core.designsystem.generated.resources.Res
import com.gmail.volkovskiyda.abit.core.designsystem.generated.resources.inter_regular
import com.gmail.volkovskiyda.abit.core.designsystem.generated.resources.inter_semibold
import org.jetbrains.compose.resources.Font

/**
 * Inter, bundled rather than assumed: Roboto exists on Android but not on macOS or in a browser, and
 * a countdown that changes metrics between platforms is the one thing this design cannot afford.
 * Licence in `composeResources/files/inter_ofl.txt`.
 */
@Composable
fun abitFontFamily(): FontFamily =
    FontFamily(
        Font(Res.font.inter_regular, FontWeight.Normal),
        Font(Res.font.inter_semibold, FontWeight.SemiBold),
    )

/**
 * Tabular numerals. Every style that carries a time or a countdown needs them, or the digits jitter
 * as they tick; exposed as one extension so a caller cannot forget which styles those are.
 */
val TextStyle.tabular: TextStyle get() = copy(fontFeatureSettings = "tnum")

@Composable
fun abitTypography(): Typography {
    val inter = abitFontFamily()
    val base = Typography()
    return Typography(
        displayLarge =
            base.displayLarge.copy(
                fontFamily = inter,
                fontSize = 56.sp,
                lineHeight = 64.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.02).em,
            ),
        // The tablet countdown. Material has no larger display role, so this is displayLarge's twin.
        displayMedium =
            base.displayMedium.copy(
                fontFamily = inter,
                fontSize = 72.sp,
                lineHeight = 80.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.02).em,
            ),
        displaySmall =
            base.displaySmall.copy(
                fontFamily = inter,
                fontSize = 40.sp,
                lineHeight = 48.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.02).em,
            ),
        headlineMedium =
            base.headlineMedium.copy(
                fontFamily = inter,
                fontSize = 24.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        titleMedium =
            base.titleMedium.copy(
                fontFamily = inter,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        bodyMedium =
            base.bodyMedium.copy(
                fontFamily = inter,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal,
            ),
        labelSmall =
            base.labelSmall.copy(
                fontFamily = inter,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.08.em,
            ),
    )
}
