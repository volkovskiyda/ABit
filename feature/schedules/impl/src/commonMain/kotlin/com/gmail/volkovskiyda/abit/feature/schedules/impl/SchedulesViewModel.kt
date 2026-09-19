package com.gmail.volkovskiyda.abit.feature.schedules.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gmail.volkovskiyda.abit.core.domain.AuthRepository
import com.gmail.volkovskiyda.abit.core.domain.AuthUser
import com.gmail.volkovskiyda.abit.core.domain.Conflict
import com.gmail.volkovskiyda.abit.core.domain.ScheduleRepository
import com.gmail.volkovskiyda.abit.core.domain.conflicts
import com.gmail.volkovskiyda.abit.core.model.Schedule
import com.gmail.volkovskiyda.abit.core.model.ScheduleId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val STOP_TIMEOUT_MILLIS = 5_000L

data class SchedulesUiState(
    val schedules: List<Schedule> = emptyList(),
    val conflicts: List<Conflict> = emptyList(),
    /**
     * False for the initial value only, which is the frame before the repository has answered.
     * Without it "no schedules" and "not asked yet" are the same empty list, and a screen that acts
     * on the difference — the conflict sheet, which dismisses itself when its conflict is gone —
     * acts on the wrong one.
     */
    val loaded: Boolean = false,
    /** `null` means signed out entirely; an anonymous user is still a user. */
    val user: AuthUser? = null,
) {
    /**
     * Nothing to show, and the repository has answered. [loaded] is the whole point: without it the
     * frame before the first emission draws the empty state and then replaces it with the list.
     */
    val isEmpty: Boolean get() = loaded && schedules.isEmpty()

    /**
     * Whether this account still has an account to gain. Signed out and anonymous are one case
     * here, as they are in Settings: neither syncs, and both are fixed by the same button.
     */
    val offersSignIn: Boolean get() = user?.isAnonymous != false

    /** The conflicts a given schedule is part of, for its card's overlap chip. */
    fun conflictsFor(id: ScheduleId): List<Conflict> = conflicts.filter { it.first == id || it.second == id }
}

class SchedulesViewModel(
    private val repository: ScheduleRepository,
    authRepository: AuthRepository,
) : ViewModel() {
    val state: StateFlow<SchedulesUiState> =
        combine(
            repository.observeSchedules(),
            // Only the empty screen reads this, to offer sync to an account that has none. It is a
            // `combine` rather than a second flow the screen collects because `loaded` has to mean
            // "everything this state says is true", and a list that arrives before the user would
            // otherwise draw one frame of the wrong empty state.
            authRepository.currentUser,
        ) { schedules, user ->
            SchedulesUiState(schedules = schedules, conflicts = schedules.conflicts(), loaded = true, user = user)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = SchedulesUiState(),
        )

    fun toggle(
        id: ScheduleId,
        enabled: Boolean,
    ) {
        viewModelScope.launch {
            val schedule = repository.findById(id) ?: return@launch
            repository.save(schedule.copy(enabled = enabled))
        }
    }

    /** Soft: the row stays with `deletedAt` stamped, so the other devices learn about it. */
    fun delete(id: ScheduleId) {
        viewModelScope.launch { repository.delete(id) }
    }

    /**
     * Resolving switches the loser **off entirely**, not for that day only — which is what the
     * conflict sheet's copy promises. Keeping it on for other days would leave the overlap in place.
     *
     * The loser is switched off **last**, and that ordering is load-bearing. Switching it off is the
     * write that makes the conflict disappear; the conflict disappearing is what closes the sheet,
     * which pops its back-stack entry, which cancels this scope. Anything queued after it would be
     * racing the teardown its own write started. Enabling the winner first can leave the overlap
     * standing for the frame between the two writes, which costs nothing: the sheet is still up.
     */
    fun resolveConflict(
        keep: ScheduleId,
        disable: ScheduleId,
    ) {
        viewModelScope.launch {
            // Idempotent on purpose: "this one stays on" is what the sheet promises, and the kept
            // schedule may itself have been switched off on another device between the two taps.
            repository.findById(keep)?.let { if (!it.enabled) repository.save(it.copy(enabled = true)) }
            repository.findById(disable)?.let { repository.save(it.copy(enabled = false)) }
        }
    }
}
