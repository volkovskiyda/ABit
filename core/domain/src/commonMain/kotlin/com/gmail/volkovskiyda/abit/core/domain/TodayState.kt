package com.gmail.volkovskiyda.abit.core.domain

import com.gmail.volkovskiyda.abit.core.model.DayOverride
import com.gmail.volkovskiyda.abit.core.model.Schedule
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val DEFAULT_LOOKAHEAD_DAYS = 7

/** When a schedule next makes a sound, for the off-hours copy ("Next: Workdays, tomorrow 09:00"). */
data class NextSession(
    val date: LocalDate,
    val at: LocalTime,
    val scheduleName: String,
)

/**
 * What every surface renders — phone, watch, tile, tray and menu bar all read this one type, so
 * they cannot disagree about which session it is.
 */
sealed interface TodayState {
    /** A session is in progress. [remaining] counts down to the end of the session, not the block. */
    data class Running(
        val plan: DayPlan,
        val session: Session,
        /** 1-based: the N in "Session N of M". */
        val sessionNumber: Int,
        val sessionCount: Int,
        val stage: BlockKind,
        val remaining: Duration,
        val nextBoundary: LocalTime,
    ) : TodayState

    /** "Pause today" is on. The plan is untouched — pausing silences the day, it does not rewrite it. */
    data class Paused(
        val plan: DayPlan,
        val resumesOn: LocalDate,
    ) : TodayState

    /** Before the first session, after the last, or on a day no schedule covers. */
    data class OffHours(
        val today: DayPlan,
        val next: NextSession?,
    ) : TodayState
}

/**
 * The single entry point every feature ViewModel calls. Pure: it takes the clock's reading rather
 * than reading the clock, so a test states the minute it means.
 */
fun todayState(
    schedules: List<Schedule>,
    overrides: Map<LocalDate, DayOverride>,
    now: LocalDateTime,
    lookaheadDays: Int = DEFAULT_LOOKAHEAD_DAYS,
): TodayState {
    val plan = schedules.planFor(now.date)

    if (overrides[now.date]?.paused == true) {
        val resumesOn = schedules.nextRunningDay(overrides, after = now.date, lookaheadDays)?.date
        return TodayState.Paused(plan, resumesOn = resumesOn ?: now.date.plus(1, DateTimeUnit.DAY))
    }

    val session = plan.sessionAt(now.time)
    if (session != null) {
        val stage = plan.blockAt(now.time)?.kind ?: BlockKind.Focus
        val remaining = (session.end.toSecondOfDay() - now.time.toSecondOfDay()).seconds
        return TodayState.Running(
            plan = plan,
            session = session,
            sessionNumber = session.index + 1,
            sessionCount = plan.sessions.size,
            stage = stage,
            remaining = remaining,
            nextBoundary = plan.nextBoundaryAfter(now.time) ?: session.end,
        )
    }

    val laterToday = plan.sessions.firstOrNull { it.start > now.time }
    val next =
        if (laterToday != null && plan.schedule != null) {
            NextSession(now.date, laterToday.start, plan.schedule.name)
        } else {
            schedules.nextRunningDay(overrides, after = now.date, lookaheadDays)?.toNextSession()
        }
    return TodayState.OffHours(today = plan, next = next)
}

/** The first of the next [lookaheadDays] days that has sessions and is not paused. */
private fun List<Schedule>.nextRunningDay(
    overrides: Map<LocalDate, DayOverride>,
    after: LocalDate,
    lookaheadDays: Int,
): DayPlan? =
    (1..lookaheadDays)
        .asSequence()
        .map { after.plus(it, DateTimeUnit.DAY) }
        .filterNot { overrides[it]?.paused == true }
        .map { planFor(it) }
        .firstOrNull { it.sessions.isNotEmpty() }

private fun DayPlan.toNextSession(): NextSession? {
    val schedule = schedule ?: return null
    val first = sessions.firstOrNull() ?: return null
    return NextSession(date, first.start, schedule.name)
}
