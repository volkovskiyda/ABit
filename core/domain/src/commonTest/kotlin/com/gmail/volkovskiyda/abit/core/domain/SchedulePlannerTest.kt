package com.gmail.volkovskiyda.abit.core.domain

import com.gmail.volkovskiyda.abit.core.model.Schedule
import com.gmail.volkovskiyda.abit.core.model.ScheduleId
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/** 2026-09-14 is a Monday, and 2026-09-19 the Saturday of the same week. */
internal val MONDAY = LocalDate(2026, 9, 14)
internal val SATURDAY = LocalDate(2026, 9, 19)

internal val TEST_UPDATED_AT: Instant = Instant.fromEpochSeconds(1_700_000_000)

internal val WORKDAYS =
    setOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
    )

@Suppress("LongParameterList")
internal fun schedule(
    id: String = "workdays",
    name: String = "Workdays",
    enabled: Boolean = true,
    days: Set<DayOfWeek> = WORKDAYS,
    start: LocalTime = LocalTime(9, 0),
    end: LocalTime = LocalTime(18, 0),
    focusMinutes: Int = 45,
    breakMinutes: Int = 15,
    updatedAt: Instant = TEST_UPDATED_AT,
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

private fun focus(
    from: String,
    to: String,
) = Block(BlockKind.Focus, LocalTime.parse(from), LocalTime.parse(to))

private fun rest(
    from: String,
    to: String,
) = Block(BlockKind.Break, LocalTime.parse(from), LocalTime.parse(to))

class SchedulePlannerTest {
    @Test
    fun `derives nine focus blocks and eight breaks from the design's own example`() {
        // Workdays 09:00-18:00 at 45/15 is the sample schedule in internal/design/app. The mockups
        // label it "8 sessions" on one screen and "12 blocks" on another; both are illustrative
        // placeholder numerals, and the rule the design states produces these numbers instead. Do
        // not "fix" the planner to agree with a screenshot.
        val plan = schedule().planFor(MONDAY)

        assertEquals(9, plan.sessions.size)
        assertEquals(9, plan.blocks.count { it.kind == BlockKind.Focus })
        assertEquals(8, plan.blocks.count { it.kind == BlockKind.Break })

        assertEquals(focus("09:00", "09:45"), plan.sessions.first().focus)
        assertEquals(rest("09:45", "10:00"), plan.sessions.first().rest)
        // The ninth focus starts at 17:00 and the day stops there: a break from 17:45 would have no
        // focus after it, and a day does not end on a break. The last quarter hour of the window is
        // simply unallocated, which is what "the last focus gets no break" costs.
        assertEquals(focus("17:00", "17:45"), plan.sessions.last().focus)
        assertNull(plan.sessions.last().rest, "the last focus gets no break")
        assertEquals(LocalTime(17, 45), plan.sessions.last().end)
    }

    @Test
    fun `plans the design's evening study schedule block by block`() {
        val evening =
            schedule(
                id = "evening",
                name = "Evening study",
                days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                start = LocalTime(17, 0),
                end = LocalTime(21, 30),
                focusMinutes = 50,
                breakMinutes = 10,
            )

        assertEquals(
            listOf(
                focus("17:00", "17:50"),
                rest("17:50", "18:00"),
                focus("18:00", "18:50"),
                rest("18:50", "19:00"),
                focus("19:00", "19:50"),
                rest("19:50", "20:00"),
                focus("20:00", "20:50"),
                rest("20:50", "21:00"),
                focus("21:00", "21:30"),
            ),
            evening.planFor(MONDAY).blocks,
        )
    }

    @Test
    fun `plans the design's saturday light schedule block by block`() {
        val light =
            schedule(
                id = "saturday",
                name = "Saturday light",
                days = setOf(DayOfWeek.SATURDAY),
                start = LocalTime(10, 0),
                end = LocalTime(14, 0),
                focusMinutes = 25,
                breakMinutes = 5,
            )

        assertEquals(
            listOf(
                focus("10:00", "10:25"),
                rest("10:25", "10:30"),
                focus("10:30", "10:55"),
                rest("10:55", "11:00"),
                focus("11:00", "11:25"),
                rest("11:25", "11:30"),
                focus("11:30", "11:55"),
                rest("11:55", "12:00"),
                focus("12:00", "12:25"),
                rest("12:25", "12:30"),
                focus("12:30", "12:55"),
                rest("12:55", "13:00"),
                focus("13:00", "13:25"),
                rest("13:25", "13:30"),
                focus("13:30", "13:55"),
            ),
            light.planFor(SATURDAY).blocks,
        )
    }

    @Test
    fun `cuts a focus block that would run past the end`() {
        val plan = schedule(start = LocalTime(9, 0), end = LocalTime(9, 30)).planFor(MONDAY)

        assertEquals(listOf(focus("09:00", "09:30")), plan.blocks)
    }

    @Test
    fun `fills the whole window with one focus block when the focus is longer than it`() {
        val plan =
            schedule(start = LocalTime(9, 0), end = LocalTime(10, 0), focusMinutes = 120)
                .planFor(MONDAY)

        assertEquals(listOf(focus("09:00", "10:00")), plan.blocks)
    }

    @Test
    fun `never ends the day on a break`() {
        // 09:00-10:00 at 25/5 would leave 09:55-10:00 for a break with no focus after it.
        val plan =
            schedule(start = LocalTime(9, 0), end = LocalTime(10, 0), focusMinutes = 25, breakMinutes = 5)
                .planFor(MONDAY)

        assertEquals(BlockKind.Focus, plan.blocks.last().kind)
        assertEquals(focus("09:30", "09:55"), plan.blocks.last())
    }

    @Test
    fun `plans nothing for data that cannot run`() {
        val cases =
            mapOf(
                "start == end" to schedule(start = LocalTime(9, 0), end = LocalTime(9, 0)),
                "start > end" to schedule(start = LocalTime(18, 0), end = LocalTime(9, 0)),
                "disabled" to schedule(enabled = false),
                "soft deleted" to schedule(deletedAt = TEST_UPDATED_AT),
            )

        cases.forEach { (why, candidate) ->
            val plan = candidate.planFor(MONDAY)
            assertTrue(plan.sessions.isEmpty(), "expected an empty plan: $why")
            assertNull(plan.schedule, "an empty plan has no schedule: $why")
        }
    }

    @Test
    fun `plans nothing on a weekday the schedule does not cover`() {
        assertTrue(schedule().planFor(SATURDAY).sessions.isEmpty())
    }

    @Test
    fun `picks the most recently updated schedule when two cover the same day`() {
        val older = schedule(id = "a", updatedAt = TEST_UPDATED_AT)
        val newer =
            schedule(
                id = "b",
                name = "Deep work",
                updatedAt = TEST_UPDATED_AT + 1.hours,
                focusMinutes = 50,
                breakMinutes = 10,
            )

        assertEquals(newer, listOf(older, newer).planFor(MONDAY).schedule)
        assertEquals(newer, listOf(newer, older).planFor(MONDAY).schedule)
    }

    @Test
    fun `breaks a tie on the schedule id so every device picks the same one`() {
        val first = schedule(id = "aaa")
        val second = schedule(id = "bbb", name = "Other")

        assertEquals(second, listOf(first, second).planFor(MONDAY).schedule)
        assertEquals(second, listOf(second, first).planFor(MONDAY).schedule)
    }

    @Test
    fun `plans nothing when no schedule covers the day`() {
        val plan = listOf(schedule(enabled = false)).planFor(MONDAY)

        assertNull(plan.schedule)
        assertTrue(plan.sessions.isEmpty())
    }

    @Test
    fun `reports the next boundary and the block a time falls in`() {
        val plan = schedule().planFor(MONDAY)

        assertEquals(BlockKind.Focus, plan.blockAt(LocalTime(9, 22))?.kind)
        assertEquals(BlockKind.Break, plan.blockAt(LocalTime(9, 50))?.kind)
        assertNull(plan.blockAt(LocalTime(8, 59)))
        assertNull(plan.blockAt(LocalTime(17, 45)), "the window is half open")
        assertEquals(LocalTime(9, 0), plan.nextBoundaryAfter(LocalTime(8, 0)))
        assertEquals(LocalTime(9, 45), plan.nextBoundaryAfter(LocalTime(9, 22)))
        assertEquals(LocalTime(17, 45), plan.nextBoundaryAfter(LocalTime(17, 0)))
        assertNull(plan.nextBoundaryAfter(LocalTime(17, 46)))
    }
}
