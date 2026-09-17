package com.gmail.volkovskiyda.abit.ui

import androidx.compose.ui.geometry.CornerRadius
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
 * **The image is wider than it is tall, and auto-size is off** ([main] again). AWT's two sizings
 * are not a preference: `autosize` forces the image into the menu bar's *square*, while without it
 * macOS scales by height and keeps the aspect. The square is what the highlight cannot live in —
 * a selection that stops at the glyph's edge reads as a box drawn round the icon rather than as the
 * item being lit — so the image carries [PADDING_PX] of its own on each side and the fill spans all
 * of it. The content stays square within that, laid out about the middle, so the item keeps one
 * width whether or not it is carrying minutes.
 *
 * Its pixels are twice the size the menu bar shows, which is what stops a Retina display scaling
 * one of ours up: macOS scales by `MIN(1.0, thickness / height)`, so an image already at the bar's
 * height is left at native size and drawn soft, while twice that comes back down crisply.
 *
 * **Colour is not this painter's to choose.** The item is handed to macOS as a *template* image
 * (see [main]), which means only the alpha channel survives: the system tints the result with the
 * colour the menu bar wants, white on a dark bar and black on a light one, and follows the user
 * changing it. Drawing in anything but opaque ink would therefore be drawing in vain — and drawing
 * in black, as this did before templates were turned on, left the dial all but invisible against a
 * dark menu bar.
 *
 * [highlighted] is the selected look macOS gives a menu bar extra whose menu is open, drawn here
 * because the system draws it only for a native popup menu — which this app does not use, its item
 * opening a popover on one click instead. The real one is a *translucent* fill that the glyph stays
 * solid against, so this is the same: a rounded rect at [HIGHLIGHT_ALPHA], which a template image
 * renders as exactly that tint. An opaque fill would mask the whole square and tint it into a
 * featureless block, which is what it looked like.
 */
class AbitTrayPainter(
    private val minutes: Int?,
    private val mode: BlockKind?,
    private val color: Color = Color.Black,
    private val highlighted: Boolean = false,
) : Painter() {
    override val intrinsicSize: Size = Size(CONTENT_PX + PADDING_PX * 2f, CONTENT_PX)

    override fun DrawScope.onDraw() {
        if (highlighted) {
            drawRoundRect(
                color = color.copy(alpha = HIGHLIGHT_ALPHA),
                cornerRadius = CornerRadius(size.minDimension * HIGHLIGHT_RADIUS_SHARE),
            )
        }
        // Square, centred: the padding is the item's, not the glyph's, and the dial must not drift
        // off centre when the minutes appear beside it.
        val content = size.height
        val left = (size.width - content) / 2f
        val glyphSize = if (minutes == null) content else content * GLYPH_SHARE
        val unit = glyphSize / GLYPH_UNITS
        drawDial(Offset(left + glyphSize / 2f, size.height / 2f), unit)
        if (minutes != null) drawMinutes(minutes, left + glyphSize, content - glyphSize, unit)
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
        start: Float,
        width: Float,
        unit: Float,
    ) {
        val digits = minutes.coerceIn(0, MAX_MINUTES).toString()
        val digitWidth = width / (digits.length + 1)
        val digitHeight = size.height * DIGIT_HEIGHT_SHARE
        val top = (size.height - digitHeight) / 2f
        val stroke = Stroke(width = (DIGIT_STROKE * unit).coerceAtLeast(1f))

        digits.forEachIndexed { index, digit ->
            val left = start + index * digitWidth
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
        // Twice what the menu bar shows, so a Retina display has pixels to scale down rather than up.
        const val CONTENT_PX = 44f
        const val PADDING_PX = 8f
        const val HIGHLIGHT_RADIUS_SHARE = 0.35f
        const val HIGHLIGHT_ALPHA = 0.2f
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
