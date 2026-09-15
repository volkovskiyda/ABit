package com.gmail.volkovskiyda.abit.core.data

import com.gmail.volkovskiyda.abit.core.common.TimeProvider
import com.gmail.volkovskiyda.abit.core.database.dao.DayOverrideDao
import com.gmail.volkovskiyda.abit.core.database.model.toEntity
import com.gmail.volkovskiyda.abit.core.database.model.toModel
import com.gmail.volkovskiyda.abit.core.domain.DayOverrideRepository
import com.gmail.volkovskiyda.abit.core.model.DayOverride
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * Offline-first, like every repository here: the pause the user just tapped is in effect before any
 * network call is attempted, and reaches the other devices when sync next runs.
 */
class OfflineFirstDayOverrideRepository(
    private val dao: DayOverrideDao,
    private val timeProvider: TimeProvider,
) : DayOverrideRepository {
    override fun observeFrom(date: LocalDate): Flow<Map<LocalDate, DayOverride>> =
        dao.observeFrom(date.toEpochDays()).map { entities ->
            entities.associate { entity -> entity.toModel().let { it.date to it } }
        }

    override suspend fun setPaused(
        date: LocalDate,
        paused: Boolean,
    ) = update(date) { it.copy(paused = paused) }

    override suspend fun skipBoundary(
        date: LocalDate,
        boundary: LocalTime,
    ) = update(date) { it.copy(skippedBoundaries = it.skippedBoundaries + boundary) }

    private suspend fun update(
        date: LocalDate,
        edit: (DayOverride) -> DayOverride,
    ) {
        val existing = dao.findByEpochDay(date.toEpochDays())?.toModel()
        val base = existing ?: DayOverride(date = date, updatedAt = timeProvider.now())
        dao.upsert(edit(base).copy(updatedAt = timeProvider.now()).toEntity())
    }
}
