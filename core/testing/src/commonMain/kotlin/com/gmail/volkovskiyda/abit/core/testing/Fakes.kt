package com.gmail.volkovskiyda.abit.core.testing

import com.gmail.volkovskiyda.abit.core.common.TimeProvider
import com.gmail.volkovskiyda.abit.core.domain.AuthRepository
import com.gmail.volkovskiyda.abit.core.domain.AuthUser
import com.gmail.volkovskiyda.abit.core.domain.PomodoroSessionRepository
import com.gmail.volkovskiyda.abit.core.domain.SyncState
import com.gmail.volkovskiyda.abit.core.domain.SyncStatusRepository
import com.gmail.volkovskiyda.abit.core.model.PomodoroSession
import com.gmail.volkovskiyda.abit.core.model.UserId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Instant

/** A clock a test moves by hand. Starts at an arbitrary fixed point, never "now". */
class FakeTimeProvider(
    private var current: Instant = Instant.fromEpochSeconds(1_700_000_000),
) : TimeProvider {
    override fun now(): Instant = current

    fun advanceBy(seconds: Long) {
        current = Instant.fromEpochSeconds(current.epochSeconds + seconds)
    }
}

class FakePomodoroSessionRepository(
    initial: List<PomodoroSession> = emptyList(),
) : PomodoroSessionRepository {

    private val sessions = MutableStateFlow(initial)

    override fun observeSessions(): Flow<List<PomodoroSession>> = sessions.asStateFlow()

    override suspend fun upsert(session: PomodoroSession) {
        sessions.update { current -> current.filterNot { it.id == session.id } + session }
    }

    override suspend fun delete(id: String) {
        sessions.update { current -> current.filterNot { it.id == id } }
    }
}

class FakeAuthRepository(initial: AuthUser? = null) : AuthRepository {

    private val user = MutableStateFlow(initial)

    override val currentUser: Flow<AuthUser?> = user.asStateFlow()

    override suspend fun signInAnonymously(): Result<AuthUser> =
        AuthUser(UserId("anonymous"), isAnonymous = true)
            .also { user.value = it }
            .let { Result.success(it) }

    override suspend fun signInWithGoogle(idToken: String): Result<AuthUser> =
        AuthUser(UserId("google-user"), isAnonymous = false, email = "user@example.com")
            .also { user.value = it }
            .let { Result.success(it) }

    override suspend fun signOut() {
        user.value = null
    }
}

class FakeSyncStatusRepository(
    initial: SyncState = SyncState.Unavailable,
) : SyncStatusRepository {

    private val state = MutableStateFlow(initial)

    override val syncState: Flow<SyncState> = state.asStateFlow()

    override suspend fun syncNow() {
        state.value = SyncState.Idle(lastSyncedAt = null)
    }

    fun emit(next: SyncState) {
        state.value = next
    }
}
