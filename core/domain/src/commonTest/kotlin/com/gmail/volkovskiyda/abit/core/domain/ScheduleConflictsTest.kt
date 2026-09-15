package com.gmail.volkovskiyda.abit.core.domain

import com.gmail.volkovskiyda.abit.core.model.ScheduleId
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val MON_WED_FRI = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)

class ScheduleConflictsTest {
    @Test
    fun `finds the overlap in the design's sample data`() {
        val workdays = schedule(id = "a", name = "Workdays")
        val evening =
            schedule(
                id = "b",
                name = "Evening study",
                days = MON_WED_FRI,
                start = LocalTime(17, 0),
                end = LocalTime(21, 30),
            )

        assertEquals(
            listOf(
                Conflict(
                    first = ScheduleId("a"),
                    second = ScheduleId("b"),
                    days = MON_WED_FRI,
                    from = LocalTime(17, 0),
                    to = LocalTime(18, 0),
                ),
            ),
            listOf(workdays, evening).conflicts(),
        )
    }

    @Test
    fun `orders the pair and the list by schedule id whatever order the schedules arrive in`() {
        val first = schedule(id = "a")
        val second = schedule(id = "b")

        assertEquals(listOf(first, second).conflicts(), listOf(second, first).conflicts())
    }

    @Test
    fun `reports nothing when the windows do not overlap`() {
        val morning = schedule(id = "a", start = LocalTime(9, 0), end = LocalTime(12, 0))
        val afternoon = schedule(id = "b", start = LocalTime(13, 0), end = LocalTime(17, 0))

        assertTrue(listOf(morning, afternoon).conflicts().isEmpty())
    }

    @Test
    fun `reports nothing when the windows overlap on no shared day`() {
        val monday = schedule(id = "a", days = setOf(DayOfWeek.MONDAY))
        val tuesday = schedule(id = "b", days = setOf(DayOfWeek.TUESDAY))

        assertTrue(listOf(monday, tuesday).conflicts().isEmpty())
    }

    @Test
    fun `reports nothing when a schedule cannot run`() {
        val live = schedule(id = "a")
        val disabled = schedule(id = "b", enabled = false)
        val deleted = schedule(id = "c", deletedAt = TEST_UPDATED_AT)
        val inverted = schedule(id = "d", start = LocalTime(18, 0), end = LocalTime(9, 0))

        assertTrue(listOf(live, disabled, deleted, inverted).conflicts().isEmpty())
    }

    @Test
    fun `reports every pair when three schedules overlap`() {
        val schedules = listOf(schedule(id = "a"), schedule(id = "b"), schedule(id = "c"))

        assertEquals(
            listOf(
                ScheduleId("a") to ScheduleId("b"),
                ScheduleId("a") to ScheduleId("c"),
                ScheduleId("b") to ScheduleId("c"),
            ),
            schedules.conflicts().map { it.first to it.second },
        )
    }
}
