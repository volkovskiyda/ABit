package com.gmail.volkovskiyda.abit.core.database

import com.gmail.volkovskiyda.abit.core.database.model.toEntity
import com.gmail.volkovskiyda.abit.core.database.model.toModel
import com.gmail.volkovskiyda.abit.core.model.DayOverride
import com.gmail.volkovskiyda.abit.core.model.Schedule
import com.gmail.volkovskiyda.abit.core.model.ScheduleId
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

private val UPDATED_AT = Instant.fromEpochMilliseconds(1_700_000_000_000)

class SchedulesMappingTest {
    private fun schedule(
        days: Set<DayOfWeek>,
        deletedAt: Instant? = null,
    ) = Schedule(
        id = ScheduleId("workdays"),
        name = "Workdays",
        enabled = true,
        days = days,
        start = LocalTime(9, 0),
        end = LocalTime(18, 0),
        focusMinutes = 45,
        breakMinutes = 15,
        updatedAt = UPDATED_AT,
        deletedAt = deletedAt,
    )

    @Test
    fun `round trips a schedule through the database shape`() {
        val original = schedule(days = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))

        assertEquals(original, original.toEntity().toModel())
    }

    @Test
    fun `round trips an empty day set and every day`() {
        assertEquals(emptySet(), schedule(days = emptySet()).toEntity().toModel().days)
        assertEquals(DayOfWeek.entries.toSet(), schedule(days = DayOfWeek.entries.toSet()).toEntity().toModel().days)
    }

    @Test
    fun `packs Monday into bit zero and Sunday into bit six`() {
        assertEquals(0b0000001, schedule(days = setOf(DayOfWeek.MONDAY)).toEntity().daysMask)
        assertEquals(0b1000000, schedule(days = setOf(DayOfWeek.SUNDAY)).toEntity().daysMask)
        assertEquals(0b0011111, schedule(days = WORKDAY_SET).toEntity().daysMask)
    }

    @Test
    fun `round trips a soft-deleted schedule and a live one`() {
        val deleted = schedule(days = WORKDAY_SET, deletedAt = UPDATED_AT)

        assertEquals(deleted, deleted.toEntity().toModel())
        assertEquals(null, schedule(days = WORKDAY_SET).toEntity().deletedAtMillis)
    }

    @Test
    fun `round trips a day override, boundaries and all`() {
        val original =
            DayOverride(
                date = LocalDate(2026, 9, 14),
                paused = true,
                skippedBoundaries = setOf(LocalTime(9, 45), LocalTime(10, 0)),
                updatedAt = UPDATED_AT,
            )

        assertEquals(original, original.toEntity().toModel())
        assertEquals("585,600", original.toEntity().skippedBoundaries)
    }

    @Test
    fun `round trips a day override with no skipped boundaries`() {
        val original = DayOverride(date = LocalDate(2026, 9, 14), updatedAt = UPDATED_AT)

        assertEquals("", original.toEntity().skippedBoundaries)
        assertEquals(original, original.toEntity().toModel())
    }

    private companion object {
        val WORKDAY_SET =
            setOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
            )
    }
}
