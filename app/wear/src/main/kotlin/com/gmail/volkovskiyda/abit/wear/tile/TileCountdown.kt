package com.gmail.volkovskiyda.abit.wear.tile

import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInstant
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInt32
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString
import androidx.wear.protolayout.types.LayoutString
import androidx.wear.protolayout.types.asLayoutString
import androidx.wear.protolayout.types.stringLayoutConstraint
import com.gmail.volkovskiyda.abit.core.designsystem.countdown
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

private const val SECONDS_IN_MINUTE = 60
private const val SECONDS_IN_HOUR = 3600

/** Widest `mm:ss` and widest `h:mm:ss`. The renderer reserves this much and centres the digits in it. */
private const val WIDEST_MINUTES = "88:88"
private const val WIDEST_HOURS = "8:88:88"

/**
 * The tile's clock, as expressions the **renderer** evaluates rather than numbers the service
 * computes.
 *
 * A tile is built by a service the system wakes on a schedule, so anything printed as a plain string
 * is only as honest as the next refresh — and refreshing once a minute all day to keep a countdown
 * true is exactly the kind of thing that empties a watch battery. ProtoLayout's platform clock is
 * the way out: bound to a layout field, it is re-evaluated once a second while the tile is on screen
 * and costs nothing while it is not.
 *
 * Every value here is floored at zero, so the seconds between a boundary passing and the service
 * being asked for a new tile read as `00:00` rather than as a negative.
 */
internal class TileCountdown(
    private val zone: TimeZone,
    private val today: LocalDate,
) {
    /** Seconds from now until [time] today. */
    fun secondsUntil(time: LocalTime): DynamicInt32 = secondsUntil(LocalDateTime(today, time))

    fun secondsUntil(at: LocalDateTime): DynamicInt32 {
        val target = java.time.Instant.ofEpochSecond(at.toInstant(zone).epochSeconds)
        return DynamicInstant
            .platformTimeWithSecondsPrecision()
            .durationUntil(DynamicInstant.withSecondsPrecision(target))
            .toIntSeconds()
            .atLeastZero()
    }

    /**
     * The countdown to [at] as the digits the app prints: `mm:ss`, and `h:mm:ss` once there is an
     * hour or more to go. The switch between the two is itself an expression, so a block longer than
     * an hour drops the hours field as it passes it instead of reading `0:04:12` to the end.
     *
     * [remaining] is the same countdown measured now, and is what a renderer that cannot resolve the
     * expression falls back to.
     */
    fun countdownText(
        at: LocalDateTime,
        remaining: Duration,
    ): LayoutString {
        val left = secondsUntil(at)
        val hours = left.div(SECONDS_IN_HOUR)
        val seconds = left.rem(SECONDS_IN_MINUTE).padded()
        val minutesWithinHour = left.rem(SECONDS_IN_HOUR).div(SECONDS_IN_MINUTE).padded()
        val totalMinutes = left.div(SECONDS_IN_MINUTE).padded()

        val short = totalMinutes.colon(seconds)
        val long = hours.format().colon(minutesWithinHour).colon(seconds)
        val text = DynamicString.onCondition(hours.gt(0)).use(long).elseUse(short)

        // The reserved width has to hold every value the tile will show before it is rebuilt, and the
        // countdown only ever shrinks — so whichever form fits *now* fits for the rest of its life.
        val widest = if (remaining >= 1.hours) WIDEST_HOURS else WIDEST_MINUTES
        return text.asLayoutString(
            staticValue = countdown(remaining),
            layoutConstraint = stringLayoutConstraint(widest),
        )
    }

    /** The same countdown as a plain [Duration], for the callers that want to reason about it. */
    fun remainingAt(
        at: LocalDateTime,
        now: LocalDateTime,
    ): Duration = (at.toInstant(zone) - now.toInstant(zone)).coerceAtLeast(0.seconds)
}

internal val COLON = DynamicString.constant(":")

internal fun DynamicString.colon(next: DynamicString): DynamicString = concat(COLON).concat(next)

internal fun DynamicInt32.padded(): DynamicString =
    format(
        DynamicInt32.IntFormatter
            .Builder()
            .setMinIntegerDigits(2)
            .setGroupingUsed(false)
            .build(),
    )

internal fun DynamicInt32.atLeastZero(): DynamicInt32 = DynamicInt32.onCondition(lt(0)).use(0).elseUse(this)
