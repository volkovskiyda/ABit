package com.gmail.volkovskiyda.abit.core.data

import com.gmail.volkovskiyda.abit.core.common.TimeProvider
import com.gmail.volkovskiyda.abit.core.database.dao.ScheduleDao
import com.gmail.volkovskiyda.abit.core.database.model.toEntity
import com.gmail.volkovskiyda.abit.core.database.model.toModel
import com.gmail.volkovskiyda.abit.core.domain.ScheduleRepository
import com.gmail.volkovskiyda.abit.core.model.Schedule
import com.gmail.volkovskiyda.abit.core.model.ScheduleId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Offline-first by construction: every read comes from the local database and none waits on the
 * network, so a screen renders the moment it is composed whether or not the device is online.
 *
 * Writes go to the database too, stamped with `updatedAt` here rather than by the caller. Getting
 * them to Firestore, and other devices' writes back here, is the sync engine's job.
 */
class OfflineFirstScheduleRepository(
    private val dao: ScheduleDao,
    private val timeProvider: TimeProvider,
) : ScheduleRepository {
    override fun observeSchedules(): Flow<List<Schedule>> = dao.observeAll().map { entities -> entities.map { it.toModel() } }

    override suspend fun findById(id: ScheduleId): Schedule? = dao.findById(id.value)?.toModel()

    override suspend fun save(schedule: Schedule) {
        dao.upsert(schedule.copy(updatedAt = timeProvider.now()).toEntity())
    }

    override suspend fun delete(id: ScheduleId) {
        val existing = dao.findById(id.value) ?: return
        val now = timeProvider.now()
        dao.upsert(existing.copy(updatedAtMillis = now.toEpochMilliseconds(), deletedAtMillis = now.toEpochMilliseconds()))
    }
}
