package com.gmail.volkovskiyda.abit.wear.tile

import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.types.LayoutString
import androidx.wear.protolayout.types.layoutString
import com.gmail.volkovskiyda.abit.core.designsystem.hhmm
import com.gmail.volkovskiyda.abit.core.domain.NextSession
import com.gmail.volkovskiyda.abit.core.domain.TodayState
import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/** Stands in for a number there is none of: no session today, or none scheduled at all. */
private const val EM_DASH = "—"

/**
 * The number in the middle of the ring, and how big it is allowed to be.
 *
 * A dial has a circle's worth of room, not a rectangle's: at the widest the digits may be about
 * five sixths of the ring's inner width before they start to crowd its edge. `h:mm:ss` is two
 * characters wider than `mm:ss`, so it takes the next size down rather than the same size overflowing
 * — which is why the size is decided here, beside the string, instead of at the call site.
 */
internal class Headline(
    val text: LayoutString,
    val typography: Int,
)

private fun countdownHeadline(
    live: TileCountdown,
    at: LocalDateTime,
    remaining: Duration,
): Headline =
    Headline(
        text = live.countdownText(at, remaining),
        typography = if (remaining >= 1.hours) Typography.NUMERAL_SMALL else Typography.NUMERAL_MEDIUM,
    )

private fun still(text: String): Headline = Headline(text.layoutString, Typography.NUMERAL_MEDIUM)

/**
 * Live while it is worth watching — inside a session, and before the first one of a day that has
 * already started. A session on another day is a time of day instead: a watch is not the place to be
 * told there are fourteen hours to go.
 */
internal fun TodayState.headline(
    live: TileCountdown,
    now: LocalDateTime,
): Headline =
    when (this) {
        is TodayState.Running -> countdownHeadline(live, LocalDateTime(now.date, nextBoundary), stageRemaining)
        is TodayState.Skipped -> still(EM_DASH)
        is TodayState.OffHours -> next.headline(live, now)
    }

private fun NextSession?.headline(
    live: TileCountdown,
    now: LocalDateTime,
): Headline =
    when {
        this == null -> still(EM_DASH)
        date != now.date -> still(hhmm(at))
        else -> LocalDateTime(date, at).let { countdownHeadline(live, it, live.remainingAt(it, now)) }
    }
