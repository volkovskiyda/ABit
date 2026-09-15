package com.gmail.volkovskiyda.abit.core.testing

import com.gmail.volkovskiyda.abit.core.common.LocalClock
import com.gmail.volkovskiyda.abit.core.common.TimeProvider
import com.gmail.volkovskiyda.abit.core.model.Schedule
import com.gmail.volkovskiyda.abit.core.model.ScheduleId
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Instant

/** The design's own sample schedule, shared by every test that needs one that actually runs. */
val WORKDAY_SET: Set<DayOfWeek> =
    setOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
    )

private const val NINE_AM_HOUR = 9
private const val SIX_PM_HOUR = 18
private const val DEFAULT_FOCUS_MINUTES = 45
private const val DEFAULT_BREAK_MINUTES = 15

@Suppress("LongParameterList")
fun testSchedule(
    id: String = "workdays",
    name: String = "Workdays",
    enabled: Boolean = true,
    days: Set<DayOfWeek> = WORKDAY_SET,
    start: LocalTime = LocalTime(NINE_AM_HOUR, 0),
    end: LocalTime = LocalTime(SIX_PM_HOUR, 0),
    focusMinutes: Int = DEFAULT_FOCUS_MINUTES,
    breakMinutes: Int = DEFAULT_BREAK_MINUTES,
    updatedAt: Instant = TEST_EPOCH,
    deletedAt: Instant? = null,
) = Schedule(
    id = ScheduleId(id),
    name = name,
    enabled = enabled,
    days = days,
    start = start,
    end = end,
    focusMinutes = focusMinutes,
    breakMinutes = breakMinutes,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

/** A clock pinned to one wall-clock instant in UTC, which is what a ViewModel test usually wants. */
fun fixedClock(
    date: LocalDate,
    time: LocalTime,
): LocalClock {
    val instant = LocalDateTime(date, time).toInstant(TimeZone.UTC)
    return LocalClock(TimeProvider { instant }, FakeTimeZoneProvider())
}
