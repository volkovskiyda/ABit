package com.gmail.volkovskiyda.abit.core.model

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlinx.datetime.isoDayNumber
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.time.Instant

@JvmInline
@Serializable
value class ScheduleId(
    val value: String,
)

/**
 * A recurring chime window: on these weekdays, between these two wall-clock times, alternate
 * [focusMinutes] of focus with [breakMinutes] of break and chime at every boundary.
 *
 * [start] and [end] are **local wall-clock plus weekday, never instants**. Travelling keeps the
 * chimes at the same local hour, which is the whole point of the model. The blocks themselves are
 * derived on demand and never stored — see `Schedule.planFor` in `core:domain`.
 */
@Serializable
data class Schedule(
    val id: ScheduleId,
    val name: String,
    val enabled: Boolean,
    val days: Set<DayOfWeek>,
    val start: LocalTime,
    val end: LocalTime,
    val focusMinutes: Int,
    val breakMinutes: Int,
    /** Last local modification, and the field last-write-wins reconciliation compares on. */
    val updatedAt: Instant,
    /** Soft delete. Non-null rows are filtered out of every read; sync still carries them. */
    val deletedAt: Instant? = null,
)

/** The editor's stepper rules, in one place so a screen and a validator cannot disagree. */
val FOCUS_MINUTES_RANGE = 5..120

val BREAK_MINUTES_RANGE = 5..120

const val LENGTH_STEP_MINUTES = 5

/**
 * The one encoding of a weekday set and a wall-clock time that both stores use — SQLite columns and
 * the Firestore wire format. Keeping it here rather than in either of them is what stops the two
 * drifting into disagreeing about which bit is Monday.
 *
 * Mon = bit 0 … Sun = bit 6.
 */
fun Set<DayOfWeek>.toDaysMask(): Int = fold(0) { mask, day -> mask or (1 shl (day.isoDayNumber - 1)) }

fun Int.toDayOfWeekSet(): Set<DayOfWeek> = DayOfWeek.entries.filterTo(mutableSetOf()) { this and (1 shl (it.isoDayNumber - 1)) != 0 }

fun LocalTime.toMinuteOfDay(): Int = toSecondOfDay() / SECONDS_IN_MINUTE

fun localTimeOfMinute(minuteOfDay: Int): LocalTime = LocalTime.fromSecondOfDay(minuteOfDay * SECONDS_IN_MINUTE)

internal const val SECONDS_IN_MINUTE = 60
