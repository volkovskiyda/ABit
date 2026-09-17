package com.gmail.volkovskiyda.abit.core.domain

import com.gmail.volkovskiyda.abit.core.model.DayOverride
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun at(
    hour: Int,
    minute: Int,
    second: Int = 0,
) = LocalDateTime(MONDAY, LocalTime(hour, minute, second))

class ChimePlanTest {
    private val schedules = listOf(schedule())

    @Test
    fun `walks the day's boundaries in order`() {
        val chimes = chimesFrom(schedules, emptyMap(), at(8, 0), limit = 5)

        assertEquals(
            listOf(
                LocalTime(9, 0) to ChimeKind.FocusStart,
                LocalTime(9, 45) to ChimeKind.BreakStart,
                LocalTime(10, 0) to ChimeKind.FocusStart,
                LocalTime(10, 45) to ChimeKind.BreakStart,
                LocalTime(11, 0) to ChimeKind.FocusStart,
            ),
            chimes.map { it.at.time to it.kind },
        )
        assertTrue(chimes.all { it.scheduleName == "Workdays" })
    }

    @Test
    fun `the day's last focus end is a DayEnd, not a BreakStart`() {
        val chimes = chimesFrom(schedules, emptyMap(), at(17, 0), limit = 8)

        assertEquals(
            listOf(LocalTime(17, 45) to ChimeKind.DayEnd),
            chimes.filter { it.at.date == MONDAY }.map { it.at.time to it.kind },
        )
    }

    @Test
    fun `a boundary exactly at from is not returned, so a scheduler cannot fire it twice`() {
        val chimes = chimesFrom(schedules, emptyMap(), at(9, 45), limit = 1)

        assertEquals(LocalTime(10, 0), chimes.single().at.time)
    }

    @Test
    fun `a boundary a second away is returned`() {
        val chimes = chimesFrom(schedules, emptyMap(), at(9, 44, 59), limit = 1)

        assertEquals(LocalTime(9, 45), chimes.single().at.time)
    }

    @Test
    fun `a skipped day yields no chimes at all`() {
        val overrides = mapOf(MONDAY to DayOverride(MONDAY, skipped = true, updatedAt = TEST_UPDATED_AT))

        val chimes = chimesFrom(schedules, overrides, at(8, 0), limit = 3)

        assertTrue(chimes.none { it.at.date == MONDAY })
        assertEquals(LocalDate(2026, 9, 15), chimes.first().at.date, "the walk carries on into tomorrow")
    }

    @Test
    fun `a skipped boundary loses its sound but not its block`() {
        val overrides =
            mapOf(
                MONDAY to
                    DayOverride(
                        MONDAY,
                        skippedBoundaries = setOf(LocalTime(9, 45)),
                        updatedAt = TEST_UPDATED_AT,
                    ),
            )

        val chimes = chimesFrom(schedules, overrides, at(9, 0), limit = 2)

        assertEquals(listOf(LocalTime(10, 0), LocalTime(10, 45)), chimes.map { it.at.time })
        // The block itself is untouched: the timeline still draws the break 09:45–10:00.
        assertEquals(
            LocalTime(9, 45),
            schedules
                .planFor(MONDAY)
                .sessions
                .first()
                .rest
                ?.start,
        )
    }

    @Test
    fun `the walk crosses midnight into the next scheduled day`() {
        val chimes = chimesFrom(schedules, emptyMap(), at(22, 0), limit = 2)

        assertEquals(LocalDate(2026, 9, 15), chimes.first().at.date)
        assertEquals(LocalTime(9, 0), chimes.first().at.time)
        assertEquals(LocalTime(9, 45), chimes[1].at.time)
    }

    @Test
    fun `a schedule that never runs returns an empty list rather than looping`() {
        val chimes = chimesFrom(listOf(schedule(enabled = false)), emptyMap(), at(8, 0), limit = 8)

        assertEquals(emptyList(), chimes)
    }

    @Test
    fun `a zero limit asks for nothing and gets nothing`() {
        assertEquals(emptyList(), chimesFrom(schedules, emptyMap(), at(8, 0), limit = 0))
    }
}
