package com.gmail.volkovskiyda.abit.core.data

import com.gmail.volkovskiyda.abit.core.database.dao.PomodoroSessionDao
import com.gmail.volkovskiyda.abit.core.database.model.toEntity
import com.gmail.volkovskiyda.abit.core.database.model.toModel
import com.gmail.volkovskiyda.abit.core.domain.PomodoroSessionRepository
import com.gmail.volkovskiyda.abit.core.model.PomodoroSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Offline-first by construction: every read comes from the local database and none waits on the
 * network, so a screen renders the moment it is composed whether or not the device is online.
 *
 * Writes go to the database too. Getting them to Firestore, and other devices' writes back here, is
 * the sync engine's job (plan item 09) — this class stays the single place the app reads and writes
 * sessions either way.
 */
class OfflineFirstPomodoroSessionRepository(
    private val dao: PomodoroSessionDao,
) : PomodoroSessionRepository {
    override fun observeSessions(): Flow<List<PomodoroSession>> = dao.observeAll().map { entities -> entities.map { it.toModel() } }

    override suspend fun upsert(session: PomodoroSession) {
        dao.upsert(session.toEntity())
    }

    override suspend fun delete(id: String) {
        dao.deleteById(id)
    }
}
