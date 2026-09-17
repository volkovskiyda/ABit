package com.gmail.volkovskiyda.abit.core.designsystem

import com.gmail.volkovskiyda.abit.core.domain.Session
import kotlinx.datetime.LocalTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val FULL_TURN_DEG = 360f
private const val SECONDS_IN_MINUTE = 60

/** The gap between the two arcs, in degrees. Only drawn when both arcs are present. */
const val RING_GAP_DEGREES = 4f

/**
 * The two arcs of the session ring, both measured clockwise from 12 o'clock.
 *
 * [breakSweep] starts at 12 o'clock; [focusSweep] continues from where it ends. Either can be zero:
 * the last session of a day has no break, and once the focus is over only the break share remains.
 */
data class RingArcs(
    val breakSweep: Float,
    val focusSweep: Float,
    val gapDegrees: Float = if (breakSweep > 0f && focusSweep > 0f) RING_GAP_DEGREES else 0f,
) {
    val totalSweep: Float get() = breakSweep + focusSweep

    companion object {
        /** Nothing is running: the track is drawn and no arc is. */
        val Empty = RingArcs(breakSweep = 0f, focusSweep = 0f)
    }
}

/**
 * The ring is a countdown for the **whole session** — focus plus the break that follows it — anchored
 * at 12 o'clock and drawn clockwise. Its total sweep is proportional to the time left, so the arc
 * shrinks from its free end as the session runs out. The share nearest 12 o'clock is the break;
 * beyond it is the focus.
 *
 * The design says "the first 90°"; that is the 45/15 instance of `breakMinutes / sessionMinutes ×
 * 360°`, not a constant. A 50/10 session gives 60°, and a session with no break gives none at all.
 */
fun ringArcs(
    session: Session,
    now: LocalTime,
): RingArcs = ringArcs(session, (session.end.toSecondOfDay() - now.toSecondOfDay()).seconds)

/**
 * The same ring from the countdown a surface already holds, so a screen rendering
 * `TodayState.Running` does not have to reconstruct "now" by subtracting. Note which countdown:
 * `sessionRemaining`, never the `stageRemaining` the digits inside the ring print.
 */
fun ringArcs(
    session: Session,
    sessionRemaining: Duration,
): RingArcs {
    val sessionSeconds = session.end.toSecondOfDay() - session.start.toSecondOfDay()
    if (sessionSeconds <= 0) return RingArcs.Empty

    val remainingSeconds = sessionRemaining.inWholeSeconds.toInt().coerceIn(0, sessionSeconds)
    val breakSeconds = session.rest?.let { it.end.toSecondOfDay() - it.start.toSecondOfDay() } ?: 0

    val perSecond = FULL_TURN_DEG / sessionSeconds
    val breakSweep = minOf(remainingSeconds, breakSeconds) * perSecond
    val focusSweep = (remainingSeconds - minOf(remainingSeconds, breakSeconds)) * perSecond
    return RingArcs(breakSweep = breakSweep, focusSweep = focusSweep)
}

/**
 * Minutes remaining in the block [now] falls in — the focus, then the break — rounded down. That is
 * the same clock the countdown text prints, so a menu bar or a tile rendering this number cannot
 * disagree with the screen. The ring, and only the ring, still spans the whole session.
 */
fun minutesLeft(
    session: Session,
    now: LocalTime,
): Int {
    val stageEnd = if (now < session.focus.end) session.focus.end else session.end
    return ((stageEnd.toSecondOfDay() - now.toSecondOfDay()).coerceAtLeast(0)) / SECONDS_IN_MINUTE
}
