package com.gmail.volkovskiyda.abit.feature.today.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gmail.volkovskiyda.abit.core.common.LocalClock
import com.gmail.volkovskiyda.abit.core.domain.AuthRepository
import com.gmail.volkovskiyda.abit.core.domain.AuthUser
import com.gmail.volkovskiyda.abit.core.domain.Conflict
import com.gmail.volkovskiyda.abit.core.domain.DayOverrideRepository
import com.gmail.volkovskiyda.abit.core.domain.DayPlan
import com.gmail.volkovskiyda.abit.core.domain.ScheduleRepository
import com.gmail.volkovskiyda.abit.core.domain.SyncState
import com.gmail.volkovskiyda.abit.core.domain.SyncStatusRepository
import com.gmail.volkovskiyda.abit.core.domain.TodayState
import com.gmail.volkovskiyda.abit.core.domain.conflicts
import com.gmail.volkovskiyda.abit.core.domain.todayState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus

private const val STOP_TIMEOUT_MILLIS = 5_000L
private const val MILLIS_IN_SECOND = 1_000
private const val NANOS_IN_MILLI = 1_000_000

/** The state before the first emission: a day with nothing in it, so no surface has to handle null. */
private val EMPTY_PLAN = DayPlan(date = LocalDate(1970, 1, 1), schedule = null, sessions = emptyList())

data class TodayUiState(
    val today: TodayState = TodayState.OffHours(EMPTY_PLAN, next = null),
    /**
     * The wall-clock minute [today] was derived at. A surface that wants to know which blocks are
     * behind it cannot ask [TodayState] — only `Running` carries a boundary, and a paused or
     * finished day is exactly when "what is left" matters most.
     */
    val now: LocalTime = LocalTime(0, 0),
    val syncState: SyncState = SyncState.Unavailable,
    /** `null` means signed out entirely; an anonymous user is still a user. */
    val user: AuthUser? = null,
    /**
     * The first unresolved overlap, if any. The day is still planned — by the most recently updated
     * schedule — so this opens a sheet rather than blocking anything.
     */
    val unresolvedConflict: Conflict? = null,
    /** Set when a sign-in attempt failed, and cleared by the next one. */
    val authError: String? = null,
)

/**
 * What the Today screen renders on every platform.
 *
 * The countdown re-emits once a second while a session is running, and **the tick carries the clock's
 * reading rather than a counter**: a process that was suspended for ten minutes resumes with the
 * right number instead of one that stopped. `WhileSubscribed` stops the ticker when nothing is on
 * screen — the chime engine is what runs then, and it needs no ticker at all.
 */
class TodayViewModel(
    private val scheduleRepository: ScheduleRepository,
    private val dayOverrideRepository: DayOverrideRepository,
    syncStatusRepository: SyncStatusRepository,
    private val authRepository: AuthRepository,
    private val clock: LocalClock,
) : ViewModel() {
    private val authError = MutableStateFlow<String?>(null)

    val state: StateFlow<TodayUiState> =
        combine(
            scheduleRepository.observeSchedules(),
            dayOverrideRepository.observeFrom(clock.today()),
            syncStatusRepository.syncState,
            authRepository.currentUser,
            combine(authError, tick()) { error, now -> error to now },
        ) { schedules, overrides, syncState, user, (error, now) ->
            TodayUiState(
                today = todayState(schedules, overrides, now),
                now = now.time,
                syncState = syncState,
                user = user,
                unresolvedConflict = schedules.conflicts().firstOrNull(),
                authError = error,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = TodayUiState(),
        )

    /** Silences the rest of today. The plan is untouched — the timeline still shows every block. */
    fun pauseToday(paused: Boolean = true) {
        viewModelScope.launch { dayOverrideRepository.setPaused(clock.today(), paused) }
    }

    /** Offered in off hours, when "today" no longer means anything the user can act on. */
    fun pauseTomorrow(paused: Boolean = true) {
        viewModelScope.launch {
            dayOverrideRepository.setPaused(clock.today().plus(1, DateTimeUnit.DAY), paused)
        }
    }

    /** Suppresses the next boundary's chime only. The block itself stays, chipped as skipped. */
    fun skipNext() {
        viewModelScope.launch {
            val now = clock.now()
            val schedules = scheduleRepository.observeSchedules().first()
            val overrides = dayOverrideRepository.observeFrom(now.date).first()
            val next = todayState(schedules, overrides, now).nextBoundary() ?: return@launch
            dayOverrideRepository.skipBoundary(now.date, next)
        }
    }

    /** Lets someone use the app with no account. Their schedules stay on the device until they link one. */
    fun signInAnonymously() = attempt { authRepository.signInAnonymously() }

    /**
     * Upgrades the current anonymous account rather than replacing it, so the schedules already on the
     * device survive and start syncing. The id token comes from the platform's own sign-in UI.
     */
    fun signInWithGoogle(idToken: String) = attempt { authRepository.signInWithGoogle(idToken) }

    fun signOut() {
        viewModelScope.launch {
            authError.value = null
            authRepository.signOut()
        }
    }

    fun onAuthError(message: String) {
        authError.value = message
    }

    private fun attempt(block: suspend () -> Result<AuthUser>) {
        viewModelScope.launch {
            authError.value = null
            block().onFailure { authError.value = it.message ?: "Sign-in failed" }
        }
    }

    /**
     * Emits the clock's reading, aligned to the next whole second so the countdown changes when the
     * user expects it to rather than a fraction late.
     */
    private fun tick(): Flow<LocalDateTime> =
        flow {
            while (true) {
                val now = clock.now()
                emit(now)
                delay((MILLIS_IN_SECOND - now.nanosecond / NANOS_IN_MILLI).coerceAtLeast(1).toLong())
            }
        }
}

private fun TodayState.nextBoundary() =
    when (this) {
        is TodayState.Running -> nextBoundary
        is TodayState.OffHours -> today.boundaries.firstOrNull()
        is TodayState.Paused -> null
    }
