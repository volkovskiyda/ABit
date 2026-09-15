package com.gmail.volkovskiyda.abit.core.sync

import com.gmail.volkovskiyda.abit.core.model.DayOverride
import com.gmail.volkovskiyda.abit.core.model.Schedule
import com.gmail.volkovskiyda.abit.core.model.UserId
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/**
 * The remote half of sync, as a contract rather than a class, so the engine's reconciliation can be
 * tested without a Firestore. [FirestoreScheduleRemoteSource] is the only implementation that ships.
 */
interface ScheduleRemoteSource {
    fun observeSchedules(user: UserId): Flow<List<Schedule>>

    fun observeOverrides(
        user: UserId,
        from: LocalDate,
    ): Flow<List<DayOverride>>

    suspend fun upsertSchedule(
        user: UserId,
        schedule: Schedule,
        deviceId: String,
    )

    suspend fun upsertOverride(
        user: UserId,
        override: DayOverride,
        deviceId: String,
    )
}
