package com.gmail.volkovskiyda.abit.core.data

import com.gmail.volkovskiyda.abit.core.common.TimeProvider
import com.gmail.volkovskiyda.abit.core.common.firebaseAvailable
import com.gmail.volkovskiyda.abit.core.database.dao.PomodoroSessionDao
import com.gmail.volkovskiyda.abit.core.database.model.toEntity
import com.gmail.volkovskiyda.abit.core.database.model.toModel
import com.gmail.volkovskiyda.abit.core.domain.AuthRepository
import com.gmail.volkovskiyda.abit.core.domain.AuthUser
import com.gmail.volkovskiyda.abit.core.domain.SyncState
import com.gmail.volkovskiyda.abit.core.domain.SyncStatusRepository
import com.gmail.volkovskiyda.abit.core.model.PomodoroSession
import com.gmail.volkovskiyda.abit.core.model.UserId
import com.gmail.volkovskiyda.abit.core.sync.FirestoreSessionRemoteSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * Keeps the local database and Firestore in step for the signed-in user.
 *
 * **Last write wins, on `updatedAt`.** Every session carries the wall-clock time of its last local
 * edit; whichever copy has the later one replaces the other. That is the whole conflict policy, and
 * it is chosen rather than defaulted to: a pomodoro session is a record of something that already
 * happened, so two devices editing one is rare and the loser is a correction, not lost work. Merging
 * anything cleverer needs a product decision about what a "conflicting" session even means.
 *
 * A clock skewed between devices therefore decides conflicts, which is the price of not putting a
 * server timestamp in the document. It is small at the scale of one person's devices.
 */
class SyncEngine(
    private val dao: PomodoroSessionDao,
    private val remote: FirestoreSessionRemoteSource,
    private val authRepository: AuthRepository,
    private val timeProvider: TimeProvider,
    private val scope: CoroutineScope,
    private val deviceId: String,
) : SyncStatusRepository {
    private val state =
        MutableStateFlow<SyncState>(
            // Not an error state: a build with no Firebase credentials reports this forever and the app
            // is fully usable, offline.
            if (firebaseAvailable()) SyncState.SignedOut else SyncState.Unavailable,
        )

    override val syncState: Flow<SyncState> = state.asStateFlow()

    override suspend fun syncNow() {
        if (!firebaseAvailable()) return
        val user =
            authRepository.currentUser.firstOrNull()?.id ?: run {
                state.value = SyncState.SignedOut
                return
            }
        state.value = SyncState.Syncing
        runCatching { reconcile(user) }
            .onSuccess { state.value = SyncState.Idle(lastSyncedAt = timeProvider.now()) }
            .onFailure { state.value = SyncState.Failed(it.message ?: "Sync failed") }
    }

    /** Starts watching the signed-in user's remote sessions. Called once, from the composition root. */
    fun start() {
        if (!firebaseAvailable()) return
        // No dispatcher argument: the injected application scope already carries one, and
        // overriding it here would mean two places deciding where background work runs.
        scope.launch {
            authRepository.currentUser.collectUser()
        }
    }

    private suspend fun Flow<AuthUser?>.collectUser() {
        collect { user ->
            if (user == null) {
                state.value = SyncState.SignedOut
                return@collect
            }
            syncNow()
        }
    }

    /**
     * One pass in both directions. Deletions are deliberately not propagated: without tombstones a
     * row missing from one side is indistinguishable from one the other side has not seen yet, and
     * treating that as a delete is how sync loses data. Deleting everywhere is its own feature.
     */
    private suspend fun reconcile(user: UserId) {
        val localById = dao.observeAll().first().associate { it.id to it.toModel() }
        val remoteById = remote.observeSessions(user).first().associateBy { it.id }

        for ((id, remoteSession) in remoteById) {
            val local = localById[id]
            if (local == null || remoteSession.updatedAt > local.updatedAt) {
                dao.upsert(remoteSession.toEntity())
            }
        }

        for ((id, localSession) in localById) {
            val remoteSession = remoteById[id]
            if (remoteSession == null || localSession.updatedAt > remoteSession.updatedAt) {
                remote.upsert(user, localSession, deviceId)
            }
        }
    }

    /** Writes a session locally and pushes it, so a save is not waiting on the network to be visible. */
    suspend fun upsertAndPush(session: PomodoroSession) {
        dao.upsert(session.toEntity())
        if (!firebaseAvailable()) return
        val user = authRepository.currentUser.firstOrNull()?.id ?: return
        runCatching { remote.upsert(user, session, deviceId) }
    }
}
