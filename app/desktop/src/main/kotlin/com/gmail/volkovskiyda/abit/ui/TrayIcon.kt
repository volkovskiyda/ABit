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
import kotlin.math.cos
import kotlin.math.sin

/**
 * The menu-bar icon: the ABit dial drawn in one colour, so it works as a macOS template image.
 * Geometry mirrors design/icon/generate.py (the launcher icon); numbers are in units of the
 * 108-grid the icon is designed on, of which the glyph occupies the central 66.
 */
object TrayIcon : Painter() {
    override val intrinsicSize: Size = Size(TRAY_ICON_SIZE_PX, TRAY_ICON_SIZE_PX)

    override fun DrawScope.onDraw() {
        val u = size.minDimension / GLYPH_UNITS
        val c = center
        val ring = RING_RADIUS * u
        val arcTopLeft = Offset(c.x - ring, c.y - ring)
        val arcSize = Size(ring * 2f, ring * 2f)
        repeat(SEGMENTS) { i ->
            val centreDeg = RING_START_DEG - i * (FULL_TURN_DEG / SEGMENTS)
            val heavy = centreDeg == NEEDLE_DEG
            drawArc(
                color = COLOR,
                startAngle = -(centreDeg + HALF_SPAN_DEG),
                sweepAngle = HALF_SPAN_DEG * 2f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = (if (heavy) HEAVY_WIDTH else THIN_WIDTH) * u, cap = StrokeCap.Butt),
            )
        }
        val rad = Math.toRadians(NEEDLE_DEG.toDouble())
        val dir = Offset(cos(rad).toFloat(), -sin(rad).toFloat())
        val nrm = Offset(-dir.y, dir.x)
        val tip = c + dir * (NEEDLE_LENGTH * u)
        val needle =
            Path().apply {
                val a = c + nrm * (NEEDLE_HALF_WIDTH * u)
                val b = c - nrm * (NEEDLE_HALF_WIDTH * u)
                moveTo(a.x, a.y)
                lineTo(tip.x, tip.y)
                lineTo(b.x, b.y)
                close()
            }
        drawPath(needle, COLOR)
        drawPath(needle, COLOR, style = Stroke(width = NEEDLE_STROKE * u, join = StrokeJoin.Round))
        drawCircle(COLOR, radius = HUB_RADIUS * u, center = c)
    }

    private val COLOR = Color.Black
    private const val TRAY_ICON_SIZE_PX = 22f
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
    private const val NEEDLE_STROKE = 2f
    private const val HUB_RADIUS = 5f
}
