package com.gmail.volkovskiyda.abit.core.data

import com.gmail.volkovskiyda.abit.core.common.TimeProvider
import com.gmail.volkovskiyda.abit.core.common.TimeZoneProvider
import com.gmail.volkovskiyda.abit.core.common.firebaseAvailable
import com.gmail.volkovskiyda.abit.core.common.localNow
import com.gmail.volkovskiyda.abit.core.database.dao.DayOverrideDao
import com.gmail.volkovskiyda.abit.core.database.dao.ScheduleDao
import com.gmail.volkovskiyda.abit.core.database.model.toEntity
import com.gmail.volkovskiyda.abit.core.database.model.toModel
import com.gmail.volkovskiyda.abit.core.domain.AuthRepository
import com.gmail.volkovskiyda.abit.core.domain.AuthUser
import com.gmail.volkovskiyda.abit.core.domain.SyncState
import com.gmail.volkovskiyda.abit.core.domain.SyncStatusRepository
import com.gmail.volkovskiyda.abit.core.model.DayOverride
import com.gmail.volkovskiyda.abit.core.model.Schedule
import com.gmail.volkovskiyda.abit.core.model.UserId
import com.gmail.volkovskiyda.abit.core.sync.ScheduleRemoteSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** Long enough that a device offline for a season still learns about a deletion. */
private val TOMBSTONE_RETENTION = 90.days

/** First retry after a listener failure, doubling up to [RETRY_MAX_DELAY]. */
private val RETRY_INITIAL_DELAY = 1.seconds
private val RETRY_MAX_DELAY = 5.minutes

/** Doubling, capped. [attempt] counts from zero; the cap also keeps the shift inside an Int. */
private fun retryDelay(attempt: Long): Duration {
    val doublings = attempt.coerceAtMost(MAX_DOUBLINGS).toInt()
    return minOf(RETRY_INITIAL_DELAY * (1 shl doublings), RETRY_MAX_DELAY)
}

private const val MAX_DOUBLINGS = 20L

/** An override in the past cannot change what any surface renders; a week of slack is generous. */
private const val OVERRIDE_RETENTION_DAYS = 7

/**
 * Keeps the local database and Firestore in step for the signed-in user.
 *
 * **Last write wins, on `updatedAt`.** Every schedule and every day override carries the wall-clock
 * time of its last local edit; whichever copy has the later one replaces the other. That is the whole
 * conflict policy, and it is chosen rather than defaulted to: two devices editing the same schedule
 * inside one sync interval is rare, the loser is a correction rather than lost work, and merging
 * field by field would need a product rule for which half of an edit won — "the phone's focus length
 * and the Mac's break length" is not an answer anyone asked for.
 *
 * A clock skewed between devices therefore decides conflicts, which is the price of not putting a
 * server timestamp in the document. It is small at the scale of one person's devices.
 *
 * **Deletions propagate, because schedules carry tombstones.** A deleted schedule keeps its row with
 * `deletedAt` stamped and syncs like any other edit, so the two sides can tell "deleted" from "not
 * seen yet" — which is the distinction whose absence used to make deletion unsafe. Tombstones are
 * purged after [TOMBSTONE_RETENTION], which is the window in which a device must come online to
 * learn about a deletion rather than resurrect the row.
 *
 * **Only a Google-linked account syncs.** An anonymous uid is a device-local identity: pushing its
 * rows would strand them under an account nothing can ever sign back into. Anonymous reports
 * [SyncState.LocalOnly].
 */
@Suppress("LongParameterList")
class SyncEngine(
    private val scheduleDao: ScheduleDao,
    private val dayOverrideDao: DayOverrideDao,
    private val remote: ScheduleRemoteSource,
    private val authRepository: AuthRepository,
    private val timeProvider: TimeProvider,
    private val timeZoneProvider: TimeZoneProvider,
    private val scope: CoroutineScope,
    private val deviceId: String,
    // Injected rather than called directly so a test can exercise the keyless path, which is what a
    // fresh clone of this repository runs as.
    private val isFirebaseAvailable: () -> Boolean = ::firebaseAvailable,
) : SyncStatusRepository {
    private val state =
        MutableStateFlow<SyncState>(
            // Not an error state: a build with no Firebase credentials reports this forever and the app
            // is fully usable, offline.
            if (isFirebaseAvailable()) SyncState.SignedOut else SyncState.Unavailable,
        )

    override val syncState: Flow<SyncState> = state.asStateFlow()

    private val schedules = ScheduleSync(scheduleDao, remote, deviceId)
    private val overrides = DayOverrideSync(dayOverrideDao, remote, deviceId)

    private var watcher: Job? = null

    override suspend fun syncNow() {
        val user = signedInUser() ?: return
        state.value = SyncState.Syncing
        runCatching { reconcile(user) }
            .onSuccess { markIdle() }
            .onFailure { state.value = SyncState.Failed(it.message ?: "Sync failed") }
    }

    /**
     * Starts watching the signed-in user's remote documents. Called once, from the composition root.
     *
     * A live listener rather than a pass per auth change: "Pause today" has to go quiet on every
     * device at once, and a device that only reconciles when its own auth state changes would hear
     * about it hours later.
     */
    fun start() {
        if (!isFirebaseAvailable()) return
        // No dispatcher argument: the injected application scope already carries one, and
        // overriding it here would mean two places deciding where background work runs.
        scope.launch {
            authRepository.currentUser.collect { user -> onUser(user) }
        }
    }

    private suspend fun onUser(user: AuthUser?) {
        watcher?.cancelAndJoin()
        watcher = null
        schedules.reset()
        overrides.reset()
        when {
            user == null -> {
                state.value = SyncState.SignedOut
            }

            user.isAnonymous -> {
                state.value = SyncState.LocalOnly
            }

            else -> {
                state.value = SyncState.Syncing
                watcher = scope.launch { watch(user.id) }
            }
        }
    }

    /**
     * Four collectors: a listener per remote collection, and a pusher per local table. Each pusher
     * waits for its collection's first snapshot before it runs, or the local rows Room emits on
     * subscribe would be pushed over remote rows this device has not read yet.
     */
    private suspend fun watch(user: UserId) =
        coroutineScope {
            val schedulesPulled = CompletableDeferred<Unit>()
            val overridesPulled = CompletableDeferred<Unit>()
            val from = today()

            launch {
                remote
                    .observeSchedules(user)
                    .failing()
                    .collect { incoming ->
                        schedules.pull(incoming)
                        markIdle()
                        schedulesPulled.complete(Unit)
                    }
            }
            launch {
                remote
                    .observeOverrides(user, from)
                    .failing()
                    .collect { incoming ->
                        overrides.pull(incoming)
                        markIdle()
                        overridesPulled.complete(Unit)
                    }
            }
            launch {
                schedulesPulled.await()
                purge()
                // Room invalidates the whole table, so this re-emits when a row is tombstoned even
                // though `observeAll` filters tombstones out of what it carries.
                scheduleDao.observeAll().collect {
                    schedules.push(user)
                    markIdle()
                }
            }
            launch {
                overridesPulled.await()
                dayOverrideDao.observeFrom(from.toEpochDays()).collect {
                    overrides.push(user)
                    markIdle()
                }
            }
        }

    /**
     * Reports a listener failure and then tries again, rather than reporting it and stopping.
     *
     * `catch` ended the flow, which ended sync for the rest of the session over a dropped
     * connection — and worse if it happened before the first snapshot, because the pull gates below
     * never completed, the two pusher coroutines waited on them forever, and [watch]'s
     * `coroutineScope` never returned. Only a new sign-in got any of it back.
     *
     * The backoff is capped rather than unbounded: a listener rejected for good — no rules match,
     * no network for an hour — should keep costing one attempt every few minutes, not spin. The
     * state stays [SyncState.Failed] the whole time, so the UI keeps saying so until a snapshot
     * arrives and `markIdle` overwrites it.
     */
    private fun <T> Flow<T>.failing(): Flow<T> =
        retryWhen { cause, attempt ->
            state.value = SyncState.Failed(cause.message ?: "Sync failed")
            delay(retryDelay(attempt))
            true
        }

    /** One pass in both directions, for the explicit [syncNow]. The listener does this continuously. */
    private suspend fun reconcile(user: UserId) {
        val from = today()
        schedules.pull(remote.observeSchedules(user).first())
        overrides.pull(remote.observeOverrides(user, from).first())
        schedules.push(user)
        overrides.push(user)
        purge()
    }

    private suspend fun purge() {
        scheduleDao.purgeTombstones(before = (timeProvider.now() - TOMBSTONE_RETENTION).toEpochMilliseconds())
        dayOverrideDao.purgeBefore(before = today().plus(-OVERRIDE_RETENTION_DAYS, DateTimeUnit.DAY).toEpochDays())
    }

    /** The user sync may run for, having already reported why it may not. */
    private suspend fun signedInUser(): UserId? {
        if (!isFirebaseAvailable()) {
            state.value = SyncState.Unavailable
            return null
        }
        val user = authRepository.currentUser.firstOrNull()
        return when {
            user == null -> {
                state.value = SyncState.SignedOut
                null
            }

            user.isAnonymous -> {
                state.value = SyncState.LocalOnly
                null
            }

            else -> {
                user.id
            }
        }
    }

    private fun markIdle() {
        state.value = SyncState.Idle(lastSyncedAt = timeProvider.now())
    }

    private fun today(): LocalDate = timeProvider.localNow(timeZoneProvider).date
}

/**
 * One collection's half of the reconciliation. [known] is what this process last saw Firestore
 * holding, per document: a local row is pushed only when it is newer than that, which is what stops
 * a snapshot arriving and being echoed straight back to the server.
 */
private class ScheduleSync(
    private val dao: ScheduleDao,
    private val remote: ScheduleRemoteSource,
    private val deviceId: String,
) {
    private val known = mutableMapOf<String, Instant>()
    private val mutex = Mutex()

    suspend fun reset() = mutex.withLock { known.clear() }

    suspend fun pull(incoming: List<Schedule>) =
        mutex.withLock {
            val local = dao.allIncludingDeleted().associate { it.id to it.toModel() }
            incoming.forEach { row ->
                known[row.id.value] = row.updatedAt
                val mine = local[row.id.value]
                // A tombstone is an ordinary edit here: the newer row wins whether or not it is one.
                if (mine == null || row.updatedAt > mine.updatedAt) dao.upsert(row.toEntity())
            }
        }

    suspend fun push(user: UserId) =
        mutex.withLock {
            dao.allIncludingDeleted().map { it.toModel() }.forEach { mine ->
                val seen = known[mine.id.value]
                if (seen == null || mine.updatedAt > seen) {
                    remote.upsertSchedule(user, mine, deviceId)
                    known[mine.id.value] = mine.updatedAt
                }
            }
        }
}

/** The same reconciliation for day overrides, keyed by date rather than by schedule id. */
private class DayOverrideSync(
    private val dao: DayOverrideDao,
    private val remote: ScheduleRemoteSource,
    private val deviceId: String,
) {
    private val known = mutableMapOf<LocalDate, Instant>()
    private val mutex = Mutex()

    suspend fun reset() = mutex.withLock { known.clear() }

    suspend fun pull(incoming: List<DayOverride>) =
        mutex.withLock {
            val local = dao.all().map { it.toModel() }.associateBy { it.date }
            incoming.forEach { row ->
                known[row.date] = row.updatedAt
                val mine = local[row.date]
                if (mine == null || row.updatedAt > mine.updatedAt) dao.upsert(row.toEntity())
            }
        }

    suspend fun push(user: UserId) =
        mutex.withLock {
            dao.all().map { it.toModel() }.forEach { mine ->
                val seen = known[mine.date]
                if (seen == null || mine.updatedAt > seen) {
                    remote.upsertOverride(user, mine, deviceId)
                    known[mine.date] = mine.updatedAt
                }
            }
        }
}
