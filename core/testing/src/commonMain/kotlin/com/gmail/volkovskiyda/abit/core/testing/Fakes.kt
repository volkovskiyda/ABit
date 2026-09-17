package com.gmail.volkovskiyda.abit.core.testing

import com.gmail.volkovskiyda.abit.core.common.Logger
import com.gmail.volkovskiyda.abit.core.common.TimeProvider
import com.gmail.volkovskiyda.abit.core.common.TimeZoneProvider
import com.gmail.volkovskiyda.abit.core.domain.AuthRepository
import com.gmail.volkovskiyda.abit.core.domain.AuthUser
import com.gmail.volkovskiyda.abit.core.domain.DayOverrideRepository
import com.gmail.volkovskiyda.abit.core.domain.ScheduleRepository
import com.gmail.volkovskiyda.abit.core.domain.SyncState
import com.gmail.volkovskiyda.abit.core.domain.SyncStatusRepository
import com.gmail.volkovskiyda.abit.core.model.DayOverride
import com.gmail.volkovskiyda.abit.core.model.Schedule
import com.gmail.volkovskiyda.abit.core.model.ScheduleId
import com.gmail.volkovskiyda.abit.core.model.UserId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.time.Instant

/**
 * A fixed instant every test starts from, so a failure reads the same on every machine and on every
 * run. 2023-11-14T22:13:20Z — an arbitrary point, chosen only for being round and in the past.
 */
val TEST_EPOCH: Instant = Instant.fromEpochSeconds(1_700_000_000)

/** A clock a test moves by hand. Starts at [TEST_EPOCH], never "now". */
class FakeTimeProvider(
    private var current: Instant = TEST_EPOCH,
) : TimeProvider {
    override fun now(): Instant = current

    fun advanceBy(seconds: Long) {
        current = Instant.fromEpochSeconds(current.epochSeconds + seconds)
    }
}

/**
 * A zone a test states rather than inherits. Defaults to UTC so a schedule test reads the same on a
 * CI runner in UTC and on a laptop in Europe/Kyiv.
 */
class FakeTimeZoneProvider(
    private var zone: TimeZone = TimeZone.UTC,
) : TimeZoneProvider {
    override fun current(): TimeZone = zone

    fun moveTo(next: TimeZone) {
        zone = next
    }
}

class FakeScheduleRepository(
    initial: List<Schedule> = emptyList(),
    private val timeProvider: TimeProvider = FakeTimeProvider(),
) : ScheduleRepository {
    private val schedules = MutableStateFlow(initial)

    override fun observeSchedules(): Flow<List<Schedule>> = schedules.asStateFlow()

    override suspend fun findById(id: ScheduleId): Schedule? = schedules.value.firstOrNull { it.id == id }

    override suspend fun save(schedule: Schedule) {
        val stamped = schedule.copy(updatedAt = timeProvider.now())
        schedules.update { current -> current.filterNot { it.id == stamped.id } + stamped }
    }

    override suspend fun delete(id: ScheduleId) {
        val now = timeProvider.now()
        schedules.update { current ->
            current.map { if (it.id == id) it.copy(updatedAt = now, deletedAt = now) else it }
        }
    }
}

class FakeDayOverrideRepository(
    initial: Map<LocalDate, DayOverride> = emptyMap(),
    private val timeProvider: TimeProvider = FakeTimeProvider(),
) : DayOverrideRepository {
    private val overrides = MutableStateFlow(initial)

    override fun observeFrom(date: LocalDate): Flow<Map<LocalDate, DayOverride>> = overrides.asStateFlow()

    override suspend fun setSkipped(
        date: LocalDate,
        skipped: Boolean,
    ) = update(date) { it.copy(skipped = skipped) }

    private fun update(
        date: LocalDate,
        edit: (DayOverride) -> DayOverride,
    ) {
        overrides.update { current ->
            val base = current[date] ?: DayOverride(date = date, updatedAt = timeProvider.now())
            current + (date to edit(base).copy(updatedAt = timeProvider.now()))
        }
    }
}

class FakeAuthRepository(
    initial: AuthUser? = null,
) : AuthRepository {
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

    /**
     * Drives the flow directly, for a sequence the four methods above cannot produce — chiefly a
     * *link*, which keeps the anonymous account's uid while flipping `isAnonymous` to false. That
     * distinction is what `SyncEngine` reads to tell a link from a collision, so a test of it has
     * to be able to say which one happened.
     */
    fun emit(next: AuthUser?) {
        user.value = next
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

/**
 * Swallows what a test does not assert on and keeps what it might. A real logger in a test writes to
 * a console nobody reads; a null one would make a class that logs untestable at the one moment it
 * matters, which is when it has caught something.
 */
class FakeLogger : Logger {
    val errors = mutableListOf<String>()

    override fun debug(
        tag: String,
        message: String,
    ) = Unit

    override fun info(
        tag: String,
        message: String,
    ) = Unit

    override fun warn(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) = Unit

    override fun error(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        errors += message
    }
}
