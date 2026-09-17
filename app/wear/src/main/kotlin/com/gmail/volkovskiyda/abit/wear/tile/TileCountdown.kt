package com.gmail.volkovskiyda.abit.wear.tile

import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInstant
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInt32
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString
import androidx.wear.protolayout.types.LayoutString
import androidx.wear.protolayout.types.asLayoutString
import androidx.wear.protolayout.types.stringLayoutConstraint
import com.gmail.volkovskiyda.abit.core.designsystem.countdown
import com.gmail.volkovskiyda.abit.core.designsystem.hhmmss
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
private const val SECONDS_IN_DAY = 86_400

/** Widest `mm:ss` and widest `h:mm:ss`. The renderer reserves this much and centres the digits in it. */
private const val WIDEST_MINUTES = "88:88"
private const val WIDEST_HOURS = "8:88:88"

/** The wall clock never narrows, so it reserves exactly what it draws. */
private const val WIDEST_CLOCK = "88:88:88"

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

    fun secondsUntil(at: LocalDateTime): DynamicInt32 =
        DynamicInstant
            .platformTimeWithSecondsPrecision()
            .durationUntil(DynamicInstant.withSecondsPrecision(at.javaInstant()))
            .toIntSeconds()
            .atLeastZero()

    /** Seconds precision is all [DynamicInstant] carries, so that is all this conversion keeps. */
    private fun LocalDateTime.javaInstant(): java.time.Instant = java.time.Instant.ofEpochSecond(toInstant(zone).epochSeconds)

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

    /**
     * The wall clock, to the second.
     *
     * Counted from **local midnight** rather than from the epoch, which is not a stylistic choice:
     * `toIntSeconds` answers a `DynamicInt32`, and seconds since 1970 will overflow one of those in
     * 2038. Seconds since midnight is a number that cannot, and it is the second-of-day the clock
     * wants anyway. The modulo carries it over midnight, so the tile does not read 24:01 in the
     * minute before the service is next woken.
     *
     * The zone's offset is the one fixed at build time. A tile that happens to be on screen through
     * a DST change shows the old offset until the next rebuild, which is a truthful description of
     * a tile: everything it knows, it knew when it was built.
     */
    fun clockText(now: LocalDateTime): LayoutString {
        val secondOfDay = secondsSince(LocalDateTime(today, LocalTime(0, 0))).rem(SECONDS_IN_DAY)
        val text =
            secondOfDay
                .div(SECONDS_IN_HOUR)
                .padded()
                .colon(secondOfDay.rem(SECONDS_IN_HOUR).div(SECONDS_IN_MINUTE).padded())
                .colon(secondOfDay.rem(SECONDS_IN_MINUTE).padded())
        return text.asLayoutString(
            staticValue = hhmmss(now.time),
            layoutConstraint = stringLayoutConstraint(WIDEST_CLOCK),
        )
    }

    /** Seconds elapsed since [at], which for a moment already past is never negative. */
    private fun secondsSince(at: LocalDateTime): DynamicInt32 =
        DynamicInstant
            .withSecondsPrecision(at.javaInstant())
            .durationUntil(DynamicInstant.platformTimeWithSecondsPrecision())
            .toIntSeconds()

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
