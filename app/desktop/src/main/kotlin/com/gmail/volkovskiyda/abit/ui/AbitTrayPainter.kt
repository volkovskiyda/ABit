package com.gmail.volkovskiyda.abit.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import com.gmail.volkovskiyda.abit.core.domain.BlockKind
import kotlin.math.cos
import kotlin.math.sin

/**
 * The menu-bar item: the ABit dial, and beside it the minutes left in the session.
 *
 * The minutes are **drawn into the image** because there is nowhere else to put them. Compose
 * Desktop's `Tray` is a thin wrapper over `java.awt.TrayIcon` — confirmed by decompiling
 * `ui-desktop-1.12.0.jar` — whose only text is a tooltip; macOS shows no title for an AWT tray icon.
 *
 * `Tray` also calls `setImageAutoSize(true)`, which scales whatever image it is given to the menu
 * bar's square. A wide glyph-plus-minutes image therefore has to be square-padded here rather than
 * handed over at its natural aspect: [intrinsicSize] stays square and the content is laid out inside
 * it. The alternative — bypassing the composable for raw `java.awt.SystemTray` with auto-size off —
 * is written up in the item and stays the fallback if this reads badly on a real menu bar.
 */
class AbitTrayPainter(
    private val minutes: Int?,
    private val mode: BlockKind?,
    private val color: Color = Color.Black,
) : Painter() {
    override val intrinsicSize: Size = Size(TRAY_ICON_SIZE_PX, TRAY_ICON_SIZE_PX)

    override fun DrawScope.onDraw() {
        val glyphSize = if (minutes == null) size.minDimension else size.minDimension * GLYPH_SHARE
        val unit = glyphSize / GLYPH_UNITS
        val centre = Offset(glyphSize / 2f, size.height / 2f)
        drawDial(centre, unit)
        if (minutes != null) drawMinutes(minutes, glyphSize, unit)
    }

    private fun DrawScope.drawDial(
        centre: Offset,
        unit: Float,
    ) {
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
        drawPath(needle, color, style = Stroke(width = NEEDLE_STROKE * unit, join = StrokeJoin.Round))
        drawCircle(color, radius = HUB_RADIUS * unit, center = centre)
    }

    /**
     * Seven-segment-ish bars rather than a `TextMeasurer`: a `Painter` has no font context of its own,
     * and two digits drawn from rectangles are legible at 22 px in a way a hinted font is not
     * guaranteed to be.
     */
    private fun DrawScope.drawMinutes(
        minutes: Int,
        glyphSize: Float,
        unit: Float,
    ) {
        val digits = minutes.coerceIn(0, MAX_MINUTES).toString()
        val digitWidth = (size.width - glyphSize) / (digits.length + 1)
        val digitHeight = size.height * DIGIT_HEIGHT_SHARE
        val top = (size.height - digitHeight) / 2f
        val stroke = Stroke(width = (DIGIT_STROKE * unit).coerceAtLeast(1f))

        digits.forEachIndexed { index, digit ->
            val left = glyphSize + index * digitWidth
            drawDigit(digit, Offset(left, top), Size(digitWidth * DIGIT_WIDTH_SHARE, digitHeight), stroke)
        }
    }

    private fun DrawScope.drawDigit(
        digit: Char,
        origin: Offset,
        digitSize: Size,
        stroke: Stroke,
    ) {
        val segments = SEVEN_SEGMENT[digit] ?: return
        val w = digitSize.width
        val h = digitSize.height
        val mid = origin.y + h / 2f
        // a b c d e f g, in the usual seven-segment order.
        val lines =
            listOf(
                Offset(origin.x, origin.y) to Offset(origin.x + w, origin.y),
                Offset(origin.x + w, origin.y) to Offset(origin.x + w, mid),
                Offset(origin.x + w, mid) to Offset(origin.x + w, origin.y + h),
                Offset(origin.x, origin.y + h) to Offset(origin.x + w, origin.y + h),
                Offset(origin.x, mid) to Offset(origin.x, origin.y + h),
                Offset(origin.x, origin.y) to Offset(origin.x, mid),
                Offset(origin.x, mid) to Offset(origin.x + w, mid),
            )
        segments.forEachIndexed { index, on ->
            if (on) drawLine(color, lines[index].first, lines[index].second, strokeWidth = stroke.width)
        }
    }

    /** Which mode the session is in. Unused by the glyph today; kept so the API does not churn. */
    internal fun modeOrNull(): BlockKind? = mode

    private companion object {
        const val TRAY_ICON_SIZE_PX = 22f
        const val GLYPH_UNITS = 66f
        const val GLYPH_SHARE = 0.5f
        const val SEGMENTS = 8
        const val FULL_TURN_DEG = 360f
        const val NEEDLE_DEG = 135f
        const val RING_START_DEG = 90f
        const val HALF_SPAN_DEG = 18.5f
        const val RING_RADIUS = 25f
        const val THIN_WIDTH = 5f
        const val HEAVY_WIDTH = 9f
        const val NEEDLE_LENGTH = 17f
        const val NEEDLE_HALF_WIDTH = 3f
        const val NEEDLE_STROKE = 2f
        const val HUB_RADIUS = 5f
        const val DEGREES_TO_RADIANS = 0.017453292519943295
        const val MAX_MINUTES = 99
        const val DIGIT_HEIGHT_SHARE = 0.55f
        const val DIGIT_WIDTH_SHARE = 0.7f
        const val DIGIT_STROKE = 2.5f

        val SEVEN_SEGMENT =
            mapOf(
                '0' to listOf(true, true, true, true, true, true, false),
                '1' to listOf(false, true, true, false, false, false, false),
                '2' to listOf(true, true, false, true, true, false, true),
                '3' to listOf(true, true, true, true, false, false, true),
                '4' to listOf(false, true, true, false, false, true, true),
                '5' to listOf(true, false, true, true, false, true, true),
                '6' to listOf(true, false, true, true, true, true, true),
                '7' to listOf(true, true, true, false, false, false, false),
                '8' to listOf(true, true, true, true, true, true, true),
                '9' to listOf(true, true, true, true, false, true, true),
            )
    }
}
