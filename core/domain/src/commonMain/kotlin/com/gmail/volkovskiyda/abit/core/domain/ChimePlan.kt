package com.gmail.volkovskiyda.abit.core.domain

import com.gmail.volkovskiyda.abit.core.model.DayOverride
import com.gmail.volkovskiyda.abit.core.model.Schedule
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.plus

/** How far ahead the walk will look for a day that actually runs, before giving up. */
private const val LOOKAHEAD_DAYS = 14

private const val DEFAULT_CHIME_LIMIT = 8

enum class ChimeKind {
    /** A focus block begins — at the start of the day, or after a break. */
    FocusStart,

    /** A focus block ends and its break begins. */
    BreakStart,

    /** The day's last focus ends. Nothing follows it, so this is not a [BreakStart]. */
    DayEnd,
}

data class Chime(
    val at: LocalDateTime,
    val kind: ChimeKind,
    val scheduleName: String,
    /** The session this boundary opens or closes, for the notification's caption. */
    val session: Session,
)

/**
 * Every chime from [from] forward, in order, across day boundaries, honouring pauses and skips.
 *
 * Returned as a list capped by [limit] rather than as a sequence because a caller only ever schedules
 * a handful: Android's `AlarmManager` takes one at a time, and the desktop sleeps until the first.
 *
 * A boundary exactly at [from] is **not** returned — only boundaries strictly after it. That is what
 * stops a scheduler that wakes a millisecond early from firing the same chime twice.
 */
fun chimesFrom(
    schedules: List<Schedule>,
    overrides: Map<LocalDate, DayOverride>,
    from: LocalDateTime,
    limit: Int = DEFAULT_CHIME_LIMIT,
): List<Chime> {
    if (limit <= 0) return emptyList()
    val chimes = mutableListOf<Chime>()

    var date = from.date
    var day = 0
    while (day <= LOOKAHEAD_DAYS && chimes.size < limit) {
        chimes += schedules.chimesOn(date, overrides, after = if (date == from.date) from.time else null)
        if (chimes.size >= limit) break
        date = date.plus(1, DateTimeUnit.DAY)
        day++
    }
    return chimes.take(limit)
}

private fun List<Schedule>.chimesOn(
    date: LocalDate,
    overrides: Map<LocalDate, DayOverride>,
    after: kotlinx.datetime.LocalTime?,
): List<Chime> {
    // A skipped day makes no sound at all. The plan is untouched: the timeline still draws it.
    if (overrides[date]?.skipped == true) return emptyList()

    val plan = planFor(date)
    val schedule = plan.schedule ?: return emptyList()
    val skipped = overrides[date]?.skippedBoundaries.orEmpty()

    return plan.sessions
        .flatMap { session -> session.boundaries(date, schedule.name, plan.sessions.size) }
        .filter { chime -> after == null || chime.at.time > after }
        // Skipping suppresses the sound, not the block: the timeline keeps the row and chips it.
        .filterNot { chime -> chime.at.time in skipped }
}

private fun Session.boundaries(
    date: LocalDate,
    scheduleName: String,
    sessionCount: Int,
): List<Chime> {
    val starts = Chime(LocalDateTime(date, focus.start), ChimeKind.FocusStart, scheduleName, this)
    val rest = rest
    return if (rest != null) {
        listOf(starts, Chime(LocalDateTime(date, rest.start), ChimeKind.BreakStart, scheduleName, this))
    } else {
        // No break after this focus means it is the day's last block, whatever its index.
        check(index == sessionCount - 1) { "only the last session of a day may have no break" }
        listOf(starts, Chime(LocalDateTime(date, focus.end), ChimeKind.DayEnd, scheduleName, this))
    }
}
