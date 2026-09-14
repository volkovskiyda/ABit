package com.gmail.volkovskiyda.abit.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter

/**
 * The menu-bar icon, drawn rather than loaded: a macOS template image has to be a single colour
 * with transparency, which is exactly what this is, and it avoids committing a binary placeholder
 * that a designed icon would replace anyway.
 */
object TrayIcon : Painter() {

    override val intrinsicSize: Size = Size(TRAY_ICON_SIZE_PX, TRAY_ICON_SIZE_PX)

    override fun DrawScope.onDraw() {
        val stroke = size.minDimension * STROKE_FRACTION
        drawCircle(
            color = Color.Black,
            radius = size.minDimension / 2f - stroke / 2f,
            center = center,
            style = Stroke(width = stroke),
        )
        // A single hand at twelve o'clock: enough to read as a timer at 22 px.
        drawLine(
            color = Color.Black,
            start = center,
            end = Offset(center.x, center.y - size.minDimension * HAND_FRACTION),
            strokeWidth = stroke,
        )
    }

    private const val TRAY_ICON_SIZE_PX = 22f
    private const val STROKE_FRACTION = 0.1f
    private const val HAND_FRACTION = 0.3f
}
