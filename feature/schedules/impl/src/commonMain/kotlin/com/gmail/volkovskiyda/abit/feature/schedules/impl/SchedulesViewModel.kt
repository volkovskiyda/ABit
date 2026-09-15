package com.gmail.volkovskiyda.abit.feature.schedules.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gmail.volkovskiyda.abit.core.domain.Conflict
import com.gmail.volkovskiyda.abit.core.domain.ScheduleRepository
import com.gmail.volkovskiyda.abit.core.domain.conflicts
import com.gmail.volkovskiyda.abit.core.model.Schedule
import com.gmail.volkovskiyda.abit.core.model.ScheduleId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val STOP_TIMEOUT_MILLIS = 5_000L

data class SchedulesUiState(
    val schedules: List<Schedule> = emptyList(),
    val conflicts: List<Conflict> = emptyList(),
) {
    /** The conflicts a given schedule is part of, for its card's overlap chip. */
    fun conflictsFor(id: ScheduleId): List<Conflict> = conflicts.filter { it.first == id || it.second == id }
}

class SchedulesViewModel(
    private val repository: ScheduleRepository,
) : ViewModel() {
    val state: StateFlow<SchedulesUiState> =
        repository
            .observeSchedules()
            .map { schedules -> SchedulesUiState(schedules = schedules, conflicts = schedules.conflicts()) }
            .stateIn(
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
     */
    fun resolveConflict(
        keep: ScheduleId,
        disable: ScheduleId,
    ) {
        viewModelScope.launch {
            repository.findById(disable)?.let { repository.save(it.copy(enabled = false)) }
            // Idempotent on purpose: "this one stays on" is what the sheet promises, and the kept
            // schedule may itself have been switched off on another device between the two taps.
            repository.findById(keep)?.let { if (!it.enabled) repository.save(it.copy(enabled = true)) }
        }
    }
}
