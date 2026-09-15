package com.gmail.volkovskiyda.abit.core.domain

import com.gmail.volkovskiyda.abit.core.model.Schedule
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

private const val SECONDS_IN_MINUTE = 60

enum class BlockKind {
    Focus,
    Break,
}

/** A half-open span of the day, `[start, end)`. Never crosses midnight. */
data class Block(
    val kind: BlockKind,
    val start: LocalTime,
    val end: LocalTime,
)

/**
 * A focus block and the break that follows it. The last session of a day has no break: the schedule
 * ends when its focus is cut at the schedule's `end`, and resting afterwards is not the app's
 * business. This — not a block — is what the countdown counts down to and what "Session N of M"
 * numbers.
 */
data class Session(
    /** 0-based. The UI shows `index + 1`. */
    val index: Int,
    val focus: Block,
    val rest: Block?,
) {
    val start: LocalTime get() = focus.start
    val end: LocalTime get() = rest?.end ?: focus.end
}

/**
 * One day, as the app renders it. Derived on demand from the schedules — nothing here is stored,
 * so a schedule edit changes every past and future day at once and there is no stale copy to
 * migrate.
 */
data class DayPlan(
    val date: LocalDate,
    /** The schedule that planned the day, or null when none runs on it. */
    val schedule: Schedule?,
    val sessions: List<Session>,
) {
    val blocks: List<Block> get() = sessions.flatMap { listOfNotNull(it.focus, it.rest) }

    /**
     * Every instant a chime fires: the start of the day's first focus, each focus/break transition,
     * and the end of the last block. Sorted and distinct.
     */
    val boundaries: List<LocalTime>
        get() = blocks.flatMap { listOf(it.start, it.end) }.distinct().sorted()

    fun blockAt(time: LocalTime): Block? = blocks.firstOrNull { time >= it.start && time < it.end }

    fun sessionAt(time: LocalTime): Session? = sessions.firstOrNull { time >= it.start && time < it.end }

    fun nextBoundaryAfter(time: LocalTime): LocalTime? = boundaries.firstOrNull { it > time }
}

/**
 * Derives the day's sessions: a focus block from [Schedule.start], then alternating break and focus
 * until [Schedule.end]. The final focus is cut at `end` and gets no break after it.
 *
 * Returns an empty plan rather than throwing on data that cannot run — disabled, soft-deleted, not
 * on this weekday, or an inverted window. The editor prevents the last of those, but a row arriving
 * by sync from another device's future version has to land somewhere harmless.
 */
fun Schedule.planFor(date: LocalDate): DayPlan {
    val runs =
        enabled &&
            deletedAt == null &&
            date.dayOfWeek in days &&
            start < end &&
            focusMinutes > 0 &&
            breakMinutes > 0
    if (!runs) return DayPlan(date, schedule = null, sessions = emptyList())

    val endSecond = end.toSecondOfDay()
    val focusSeconds = focusMinutes * SECONDS_IN_MINUTE
    val breakSeconds = breakMinutes * SECONDS_IN_MINUTE

    val sessions = mutableListOf<Session>()
    var cursor = start.toSecondOfDay()
    while (cursor < endSecond) {
        val focusEnd = minOf(cursor + focusSeconds, endSecond)
        val focus = Block(BlockKind.Focus, LocalTime.fromSecondOfDay(cursor), LocalTime.fromSecondOfDay(focusEnd))

        // A break is only worth having when focus follows it, so it needs at least a further minute
        // of window after it. Without that test the day can end on a break, or on a focus block a
        // few seconds long that no one would call a focus block.
        val breakEnd = focusEnd + breakSeconds
        val rest =
            if (breakEnd + SECONDS_IN_MINUTE <= endSecond) {
                Block(BlockKind.Break, LocalTime.fromSecondOfDay(focusEnd), LocalTime.fromSecondOfDay(breakEnd))
            } else {
                null
            }

        sessions += Session(sessions.size, focus, rest)
        cursor = rest?.end?.toSecondOfDay() ?: endSecond
    }
    return DayPlan(date, schedule = this, sessions = sessions)
}

/**
 * The day as planned by the one schedule that owns it. Overlapping enabled schedules are a conflict
 * the user resolves by hand, but until they do the day still has to make a sound, so the most
 * recently updated schedule wins — the same last-write-wins rule sync already uses. Ties break on
 * [com.gmail.volkovskiyda.abit.core.model.ScheduleId], so every device picks the same one.
 */
fun List<Schedule>.planFor(date: LocalDate): DayPlan =
    filter { it.enabled && it.deletedAt == null && date.dayOfWeek in it.days }
        .maxWithOrNull(compareBy({ it.updatedAt }, { it.id.value }))
        ?.planFor(date)
        ?: DayPlan(date, schedule = null, sessions = emptyList())
