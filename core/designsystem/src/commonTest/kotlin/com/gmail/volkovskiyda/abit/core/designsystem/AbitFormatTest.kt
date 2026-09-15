package com.gmail.volkovskiyda.abit.core.designsystem

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class AbitFormatTest {
    @Test
    fun `times are 24 hour and zero padded`() {
        assertEquals("09:00", hhmm(LocalTime(9, 0)))
        assertEquals("18:30", hhmm(LocalTime(18, 30)))
        assertEquals("00:05", hhmm(LocalTime(0, 5)))
    }

    @Test
    fun `a countdown under an hour is mm ss`() {
        assertEquals("37:38", countdown(37.minutes + 38.seconds))
        assertEquals("00:09", countdown(9.seconds))
    }

    @Test
    fun `a countdown of exactly one hour crosses to h mm ss`() {
        assertEquals("59:59", countdown(1.hours - 1.seconds))
        assertEquals("1:00:00", countdown(1.hours))
        assertEquals("2:05:07", countdown(2.hours + 5.minutes + 7.seconds))
    }

    @Test
    fun `a negative countdown reads as zero rather than as a minus`() {
        assertEquals("00:00", countdown((-5).minutes))
    }

    @Test
    fun `a day label is short weekday, day, short month`() {
        assertEquals("Mon 14 Sep", dayLabel(LocalDate(2026, 9, 14)))
    }

    @Test
    fun `a time range uses an en dash with spaces`() {
        assertEquals("09:00 – 18:00", timeRange(LocalTime(9, 0), LocalTime(18, 0)))
    }
}
