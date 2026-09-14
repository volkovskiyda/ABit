package com.gmail.volkovskiyda.abit.core.data

import com.gmail.volkovskiyda.abit.core.domain.PomodoroSessionRepository
import com.gmail.volkovskiyda.abit.core.model.PomodoroSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Offline-first by construction: reads come from local storage and never wait on the network.
 *
 * Backed by an in-memory list until the Room DAO lands (plan item 07) and the Firestore sync engine
 * after it (item 09). The interface and the call sites are already right, so those items replace
 * the storage behind this class rather than changing anything above it.
 */
class OfflineFirstPomodoroSessionRepository : PomodoroSessionRepository {
    private val sessions = MutableStateFlow<List<PomodoroSession>>(emptyList())

    override fun observeSessions(): Flow<List<PomodoroSession>> = sessions.asStateFlow()

    override suspend fun upsert(session: PomodoroSession) {
        sessions.update { current ->
            current.filterNot { it.id == session.id } + session
        }
    }

    override suspend fun delete(id: String) {
        sessions.update { current -> current.filterNot { it.id == id } }
    }
}
