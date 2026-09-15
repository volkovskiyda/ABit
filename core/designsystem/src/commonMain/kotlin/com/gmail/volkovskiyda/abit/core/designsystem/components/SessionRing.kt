package com.gmail.volkovskiyda.abit.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gmail.volkovskiyda.abit.core.designsystem.LocalAbitDarkTheme
import com.gmail.volkovskiyda.abit.core.designsystem.RingArcs
import com.gmail.volkovskiyda.abit.core.designsystem.breakArcColor
import com.gmail.volkovskiyda.abit.core.designsystem.breakMutedArcColor
import com.gmail.volkovskiyda.abit.core.designsystem.focusArcColor
import com.gmail.volkovskiyda.abit.core.designsystem.focusMutedArcColor
import com.gmail.volkovskiyda.abit.core.designsystem.ringTrackColor
import com.gmail.volkovskiyda.abit.core.domain.BlockKind

/** 12 o'clock in Compose's arc coordinates, where 0° is 3 o'clock and angles run clockwise. */
private const val TWELVE_O_CLOCK = -90f

/**
 * The session ring: the time left in this session as one arc anchored at 12 o'clock, drawn clockwise
 * over a faint full track. The current stage's share is saturated and the other is its container
 * colour.
 *
 * **No animation and no glow.** The brief is explicit about it, and a stray glow was removed from the
 * watch mockups for the same reason: this thing sits on screen all day.
 */
@Composable
fun SessionRing(
    arcs: RingArcs,
    stage: BlockKind,
    modifier: Modifier = Modifier,
    diameter: Dp = 240.dp,
    strokeWidth: Dp = 8.dp,
    content: @Composable () -> Unit = {},
) {
    val dark = LocalAbitDarkTheme.current
    val focus = focusArcColor(dark)
    val rest = breakArcColor(dark)
    // All five from the tokens, never from `MaterialTheme.colorScheme` — this composable is shared
    // with the watch, which runs Wear's Material 3 and so has no multiplatform scheme to read.
    val focusMuted = focusMutedArcColor(dark)
    val restMuted = breakMutedArcColor(dark)
    val track = ringTrackColor(dark)

    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(diameter)) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            val inset = strokeWidth.toPx() / 2f
            val topLeft = Offset(inset, inset)
            val arcSize = Size(size.width - strokeWidth.toPx(), size.height - strokeWidth.toPx())

            drawArc(
                color = track,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )

            fun arc(
                color: Color,
                from: Float,
                sweep: Float,
            ) {
                if (sweep <= 0f) return
                drawArc(
                    color = color,
                    startAngle = TWELVE_O_CLOCK + from,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke,
                )
            }

            val onBreak = stage == BlockKind.Break
            arc(if (onBreak) rest else restMuted, from = 0f, sweep = arcs.breakSweep)
            arc(
                color = if (onBreak) focusMuted else focus,
                from = arcs.breakSweep + arcs.gapDegrees,
                sweep = (arcs.focusSweep - arcs.gapDegrees).coerceAtLeast(0f),
            )
        }
        content()
    }
}
