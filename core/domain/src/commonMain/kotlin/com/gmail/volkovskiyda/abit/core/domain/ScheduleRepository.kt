package com.gmail.volkovskiyda.abit.core.domain

import com.gmail.volkovskiyda.abit.core.model.DayOverride
import com.gmail.volkovskiyda.abit.core.model.Schedule
import com.gmail.volkovskiyda.abit.core.model.ScheduleId
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/**
 * Reads come from the local database on every platform, so a screen renders without waiting for the
 * network; `core:sync` is what reconciles that database with Firestore in the background.
 *
 * Every mutating method stamps `updatedAt` **inside the implementation**, never at the call site:
 * that is the field last-write-wins compares on, and a caller that forgets it loses the write on the
 * next sync without any error to notice.
 */
interface ScheduleRepository {
    fun observeSchedules(): Flow<List<Schedule>>

    suspend fun findById(id: ScheduleId): Schedule?

    suspend fun save(schedule: Schedule)

    /** Soft delete: stamps `deletedAt` so other devices learn about it rather than resurrecting it. */
    suspend fun delete(id: ScheduleId)
}

interface DayOverrideRepository {
    /** Keyed by date, from [date] onward — a past day cannot change what any surface renders. */
    fun observeFrom(date: LocalDate): Flow<Map<LocalDate, DayOverride>>

    /** Silences the whole day, or lifts that. The plan is untouched: every surface still draws it. */
    suspend fun setSkipped(
        date: LocalDate,
        skipped: Boolean,
    )
}
