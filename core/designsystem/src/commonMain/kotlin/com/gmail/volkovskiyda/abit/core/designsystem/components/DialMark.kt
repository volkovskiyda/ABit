package com.gmail.volkovskiyda.abit.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

// The launcher icon's geometry, in units of the 108-grid it is designed on, of which the glyph
// occupies the central 66. Kept in step with internal/design/icon/generate.py and the macOS
// TrayIcon painter by hand: three surfaces draw one mark, and it has to be the same mark.
private const val GLYPH_UNITS = 66f
private const val SEGMENTS = 8
private const val FULL_TURN_DEG = 360f
private const val NEEDLE_DEG = 135f
private const val RING_START_DEG = 90f
private const val HALF_SPAN_DEG = 18.5f
private const val RING_RADIUS = 25f
private const val THIN_WIDTH = 5f
private const val HEAVY_WIDTH = 9f
private const val NEEDLE_LENGTH = 17f
private const val NEEDLE_HALF_WIDTH = 3f
private const val HUB_RADIUS = 5f
private const val DEGREES_TO_RADIANS = 0.017453292519943295

/**
 * The app's own mark — the eight-segment dial with a needle — as a one-colour glyph.
 *
 * The Today destination uses this rather than a generic clock, which is a nit the design hand-off
 * raises about the mockups: the app has a mark, and its home screen should wear it.
 */
@Composable
fun DialMark(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = LocalContentColor.current,
) {
    Canvas(modifier.size(size)) {
        val unit = this.size.minDimension / GLYPH_UNITS
        val centre = center
        val ring = RING_RADIUS * unit
        val topLeft = Offset(centre.x - ring, centre.y - ring)
        val arcSize = Size(ring * 2f, ring * 2f)

        repeat(SEGMENTS) { index ->
            val centreDeg = RING_START_DEG - index * (FULL_TURN_DEG / SEGMENTS)
            val heavy = centreDeg == NEEDLE_DEG
            drawArc(
                color = color,
                startAngle = -(centreDeg + HALF_SPAN_DEG),
                sweepAngle = HALF_SPAN_DEG * 2f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = (if (heavy) HEAVY_WIDTH else THIN_WIDTH) * unit, cap = StrokeCap.Butt),
            )
        }

        val radians = NEEDLE_DEG * DEGREES_TO_RADIANS
        val direction = Offset(cos(radians).toFloat(), -sin(radians).toFloat())
        val normal = Offset(-direction.y, direction.x)
        val tip = centre + direction * (NEEDLE_LENGTH * unit)
        val needle =
            Path().apply {
                val a = centre + normal * (NEEDLE_HALF_WIDTH * unit)
                val b = centre - normal * (NEEDLE_HALF_WIDTH * unit)
                moveTo(a.x, a.y)
                lineTo(tip.x, tip.y)
                lineTo(b.x, b.y)
                close()
            }
        drawPath(needle, color)
        drawPath(needle, color, style = Stroke(width = 2f * unit, join = StrokeJoin.Round))
        drawCircle(color, radius = HUB_RADIUS * unit, center = centre)
    }
}
