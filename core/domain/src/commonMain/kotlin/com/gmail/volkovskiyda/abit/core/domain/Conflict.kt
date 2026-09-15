package com.gmail.volkovskiyda.abit.core.domain

import com.gmail.volkovskiyda.abit.core.model.Schedule
import com.gmail.volkovskiyda.abit.core.model.ScheduleId
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime

/**
 * Two enabled schedules that want the same minutes of the same weekday. Only one of them can plan a
 * day, so the app marks this and asks which schedule stays on; resolving switches the loser off
 * entirely, not just for that day.
 *
 * [from] and [to] are the overlapping window — what the "Overlaps Workdays · Mon, Wed, Fri
 * 17:00 – 18:00" chip renders.
 */
data class Conflict(
    val first: ScheduleId,
    val second: ScheduleId,
    val days: Set<DayOfWeek>,
    val from: LocalTime,
    val to: LocalTime,
)

/**
 * Every overlapping pair, in an order that is identical on every device: each pair is ordered by
 * [ScheduleId], and the list by the pair.
 */
fun List<Schedule>.conflicts(): List<Conflict> {
    // A schedule that cannot run cannot collide with one that can: disabled, soft-deleted and
    // inverted windows are exactly what `planFor` already treats as an empty day.
    val live = filter { it.enabled && it.deletedAt == null && it.start < it.end }
    return live
        .flatMapIndexed { index, first ->
            live.drop(index + 1).mapNotNull { second -> first conflictWith second }
        }.sortedWith(compareBy({ it.first.value }, { it.second.value }))
}

private infix fun Schedule.conflictWith(other: Schedule): Conflict? {
    val sharedDays = days intersect other.days
    if (sharedDays.isEmpty()) return null

    val from = maxOf(start, other.start)
    val to = minOf(end, other.end)
    if (from >= to) return null

    val (a, b) = if (id.value <= other.id.value) this to other else other to this
    return Conflict(first = a.id, second = b.id, days = sharedDays, from = from, to = to)
}
