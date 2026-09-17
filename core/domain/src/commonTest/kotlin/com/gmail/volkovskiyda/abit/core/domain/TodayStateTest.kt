package com.gmail.volkovskiyda.abit.core.domain

import com.gmail.volkovskiyda.abit.core.model.DayOverride
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private val TUESDAY = LocalDate(2026, 9, 15)

private fun at(
    hour: Int,
    minute: Int,
    second: Int = 0,
) = LocalDateTime(MONDAY, LocalTime(hour, minute, second))

class TodayStateTest {
    private val schedules = listOf(schedule())

    @Test
    fun `counts the focus down to its own end, and the session down to the break's`() {
        val state = todayState(schedules, emptyMap(), at(9, 22, 22))

        assertIs<TodayState.Running>(state)
        assertEquals(BlockKind.Focus, state.stage)
        assertEquals(1, state.sessionNumber)
        assertEquals(9, state.sessionCount)
        // 09:22:22 to the end of the focus at 09:45 — the 45 of a 45/15 day, not the 60.
        assertEquals(22.minutes + 38.seconds, state.stageRemaining)
        // The ring still spans the session, which ends with its break at 10:00.
        assertEquals(37.minutes + 38.seconds, state.sessionRemaining)
        assertEquals(LocalTime(9, 45), state.nextBoundary)
    }

    @Test
    fun `counts the break down on its own once the focus is over`() {
        val state = todayState(schedules, emptyMap(), at(9, 50))

        assertIs<TodayState.Running>(state)
        assertEquals(BlockKind.Break, state.stage)
        assertEquals(1, state.sessionNumber)
        assertEquals(LocalTime(10, 0), state.session.end)
        // The break ends the session, so both clocks agree for the last stretch of it.
        assertEquals(10.minutes, state.stageRemaining)
        assertEquals(10.minutes, state.sessionRemaining)
    }

    @Test
    fun `counts the day's last focus down to the schedule's end, which has no break after it`() {
        val state = todayState(schedules, emptyMap(), at(17, 30))

        assertIs<TodayState.Running>(state)
        assertEquals(BlockKind.Focus, state.stage)
        assertEquals(15.minutes, state.stageRemaining)
        assertEquals(15.minutes, state.sessionRemaining)
    }

    @Test
    fun `is off hours before the first session, pointing at today`() {
        val state = todayState(schedules, emptyMap(), at(8, 0))

        assertIs<TodayState.OffHours>(state)
        assertEquals(NextSession(MONDAY, LocalTime(9, 0), "Workdays"), state.next)
    }

    @Test
    fun `is off hours after the last session, pointing at the next day that runs`() {
        val state = todayState(schedules, emptyMap(), at(22, 10))

        assertIs<TodayState.OffHours>(state)
        assertEquals(NextSession(TUESDAY, LocalTime(9, 0), "Workdays"), state.next)
    }

    @Test
    fun `has no next session when nothing runs in the lookahead window`() {
        val state = todayState(listOf(schedule(enabled = false)), emptyMap(), at(22, 10))

        assertIs<TodayState.OffHours>(state)
        assertNull(state.next)
    }

    @Test
    fun `is skipped for the whole day when the day is skipped, and says when it resumes`() {
        val overrides = mapOf(MONDAY to DayOverride(MONDAY, skipped = true, updatedAt = TEST_UPDATED_AT))

        val state = todayState(schedules, overrides, at(9, 22))

        assertIs<TodayState.Skipped>(state)
        assertEquals(TUESDAY, state.resumesOn)
        assertEquals(9, state.plan.sessions.size, "skipping silences the day, it does not rewrite it")
    }

    @Test
    fun `skips a skipped day when looking for the next session`() {
        val overrides = mapOf(TUESDAY to DayOverride(TUESDAY, skipped = true, updatedAt = TEST_UPDATED_AT))

        val state = todayState(schedules, overrides, at(22, 10))

        assertIs<TodayState.OffHours>(state)
        assertEquals(LocalDate(2026, 9, 16), state.next?.date)
    }
}
