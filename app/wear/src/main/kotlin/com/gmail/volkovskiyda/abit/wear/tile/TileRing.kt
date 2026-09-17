package com.gmail.volkovskiyda.abit.wear.tile

import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.DimensionBuilders.AngularLayoutConstraint
import androidx.wear.protolayout.DimensionBuilders.DegreesProp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.ANGULAR_ALIGNMENT_START
import androidx.wear.protolayout.LayoutElementBuilders.ARC_ANCHOR_START
import androidx.wear.protolayout.LayoutElementBuilders.ARC_DIRECTION_CLOCKWISE
import androidx.wear.protolayout.LayoutElementBuilders.STROKE_CAP_ROUND
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat
import com.gmail.volkovskiyda.abit.core.designsystem.AbitTokens
import com.gmail.volkovskiyda.abit.core.designsystem.RING_GAP_DEGREES
import com.gmail.volkovskiyda.abit.core.designsystem.RingArcs
import com.gmail.volkovskiyda.abit.core.domain.BlockKind
import com.gmail.volkovskiyda.abit.core.domain.Session

/**
 * A hair under a full turn. An `ArcLine` of exactly 360° wraps to nothing — ProtoLayout takes the
 * length modulo a full turn — which is why the platform's own progress indicator keeps the same
 * offset. It is the ceiling for every arc here, and the full turn the dynamic maths divides.
 */
private const val RING_SWEEP_DEGREES = 359.95f

/** The faint full circle the arcs are drawn over. */
internal fun ringTrack(strokeWidthDp: Float): LayoutElementBuilders.Arc =
    ring(
        listOf(
            LayoutElementBuilders.ArcLine
                .Builder()
                .setLength(DimensionBuilders.degrees(RING_SWEEP_DEGREES))
                .setThickness(DimensionBuilders.dp(strokeWidthDp))
                .setColor(ColorBuilders.argb(AbitTokens.Dark.SURFACE_CONTAINER_HIGHEST.toInt()))
                .build(),
        ),
    )

/**
 * The session ring, the same arc the phone and the watch app draw: the time left in the **whole
 * session** anchored at 12 o'clock and running clockwise, the break's share nearest 12 and the focus
 * beyond it, shrinking from its free end. [RingArcs] is where that geometry lives, so the tile and
 * the screens cannot disagree about it.
 *
 * Both arcs carry a **dynamic** length as well as [arcs]' static one. The renderer evaluates it from
 * the platform clock once a second while the tile is on screen, so the ring empties in step with the
 * digits inside it rather than lurching when the service is next woken — and the service is not woken
 * for it at all. The static value is what an older renderer, or one that cannot resolve the
 * expression, falls back to; it is correct for the moment the tile was built.
 *
 * Which arc is saturated is decided here, at build time, from [stage] — a colour cannot be bound to
 * an expression, so the swap from tangerine to mint rides on the rebuild the freshness interval
 * schedules for that boundary anyway.
 */
internal fun sessionRing(
    session: Session,
    stage: BlockKind,
    arcs: RingArcs,
    live: TileCountdown,
    strokeWidthDp: Float,
): LayoutElementBuilders.Arc {
    val sessionSeconds = session.end.toSecondOfDay() - session.start.toSecondOfDay()
    val perSecond = RING_SWEEP_DEGREES / sessionSeconds
    val breakSeconds = session.rest?.let { it.end.toSecondOfDay() - it.start.toSecondOfDay() } ?: 0
    val breakShare = breakSeconds * perSecond
    val focusShare = RING_SWEEP_DEGREES - breakShare

    val onBreak = stage == BlockKind.Break
    val breakColor = if (onBreak) AbitTokens.Dark.TERTIARY_ARC else AbitTokens.Dark.TERTIARY_CONTAINER
    val focusColor = if (onBreak) AbitTokens.Dark.PRIMARY_CONTAINER else AbitTokens.Dark.PRIMARY

    // Both countdowns are floored at zero, so neither arc can be handed a negative length in the
    // seconds between a boundary passing and the service being asked for a new tile.
    val focusLeft = live.secondsUntil(session.focus.end)
    val sessionLeft = live.secondsUntil(session.end)
    // The break's share is whatever is left once the focus is taken out of it: constant at its full
    // width while the focus runs, and only then does it start to shrink. That is why reserving
    // [breakShare] for it below cannot move the focus arc.
    val breakSweep = sessionLeft.minus(focusLeft).asFloat().times(perSecond)
    val focusSweep = focusLeft.asFloat().times(perSecond)

    val contents = mutableListOf<LayoutElementBuilders.ArcLayoutElement>()
    if (breakSeconds > 0) {
        contents +=
            arcLine(
                static = arcs.breakSweep,
                dynamic = breakSweep,
                reserved = breakShare,
                color = breakColor,
                strokeWidthDp = strokeWidthDp,
            )
        contents +=
            LayoutElementBuilders.ArcSpacer
                .Builder()
                .setLength(DimensionBuilders.degrees(RING_GAP_DEGREES))
                .setThickness(DimensionBuilders.dp(strokeWidthDp))
                .build()
    }
    // The gap is carved out of the focus arc rather than added to the ring, so the two arcs together
    // still span the time left — the same subtraction `SessionRing` makes.
    val gap = if (breakSeconds > 0) RING_GAP_DEGREES else 0f
    contents +=
        arcLine(
            static = (arcs.focusSweep - gap).coerceIn(0f, RING_SWEEP_DEGREES),
            dynamic = focusSweep.atLeast(gap).minus(gap),
            reserved = focusShare,
            color = focusColor,
            strokeWidthDp = strokeWidthDp,
        )

    return ring(contents)
}

private fun arcLine(
    static: Float,
    dynamic: DynamicFloat,
    reserved: Float,
    color: Long,
    strokeWidthDp: Float,
): LayoutElementBuilders.ArcLine =
    LayoutElementBuilders.ArcLine
        .Builder()
        .setLength(DegreesProp.Builder(static).setDynamicValue(dynamic).build())
        // Without this the arc has no known size during the layout pass and the renderer refuses the
        // binding. START keeps the arc's own start where it is and lets its free end move, which is
        // the whole point of the shape.
        .setLayoutConstraintsForDynamicLength(
            AngularLayoutConstraint
                .Builder(reserved)
                .setAngularAlignment(ANGULAR_ALIGNMENT_START)
                .build(),
        ).setThickness(DimensionBuilders.dp(strokeWidthDp))
        .setColor(ColorBuilders.argb(color.toInt()))
        .setStrokeCap(STROKE_CAP_ROUND)
        .build()

/** Anchored at 12 o'clock and laid out clockwise, which is what makes the arcs read as a clock. */
private fun ring(contents: List<LayoutElementBuilders.ArcLayoutElement>): LayoutElementBuilders.Arc =
    LayoutElementBuilders.Arc
        .Builder()
        .setAnchorAngle(DimensionBuilders.degrees(0f))
        .setAnchorType(ARC_ANCHOR_START)
        .setArcDirection(ARC_DIRECTION_CLOCKWISE)
        .apply { contents.forEach { addContent(it) } }
        .build()

private fun DynamicFloat.atLeast(floor: Float): DynamicFloat = DynamicFloat.onCondition(lt(floor)).use(floor).elseUse(this)
