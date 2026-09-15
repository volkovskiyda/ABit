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
    fun `counts down to the end of the session, not the end of the block`() {
        val state = todayState(schedules, emptyMap(), at(9, 22, 22))

        assertIs<TodayState.Running>(state)
        assertEquals(BlockKind.Focus, state.stage)
        assertEquals(1, state.sessionNumber)
        assertEquals(9, state.sessionCount)
        // 09:22:22 to the end of session 1 at 10:00, which is the end of its break.
        assertEquals(37.minutes + 38.seconds, state.remaining)
        assertEquals(LocalTime(9, 45), state.nextBoundary)
    }

    @Test
    fun `stays in the same session through its break`() {
        val state = todayState(schedules, emptyMap(), at(9, 50))

        assertIs<TodayState.Running>(state)
        assertEquals(BlockKind.Break, state.stage)
        assertEquals(1, state.sessionNumber)
        assertEquals(LocalTime(10, 0), state.session.end)
        assertEquals(10.minutes, state.remaining)
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
    fun `is paused for the whole day when the day is paused, and says when it resumes`() {
        val overrides = mapOf(MONDAY to DayOverride(MONDAY, paused = true, updatedAt = TEST_UPDATED_AT))

        val state = todayState(schedules, overrides, at(9, 22))

        assertIs<TodayState.Paused>(state)
        assertEquals(TUESDAY, state.resumesOn)
        assertEquals(9, state.plan.sessions.size, "pausing silences the day, it does not rewrite it")
    }

    @Test
    fun `skips a paused day when looking for the next session`() {
        val overrides = mapOf(TUESDAY to DayOverride(TUESDAY, paused = true, updatedAt = TEST_UPDATED_AT))

        val state = todayState(schedules, overrides, at(22, 10))

        assertIs<TodayState.OffHours>(state)
        assertEquals(LocalDate(2026, 9, 16), state.next?.date)
    }
}
