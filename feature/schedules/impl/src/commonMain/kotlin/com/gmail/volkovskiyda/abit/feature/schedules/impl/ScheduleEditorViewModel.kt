package com.gmail.volkovskiyda.abit.feature.schedules.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gmail.volkovskiyda.abit.core.common.LocalClock
import com.gmail.volkovskiyda.abit.core.domain.BlockKind
import com.gmail.volkovskiyda.abit.core.domain.Conflict
import com.gmail.volkovskiyda.abit.core.domain.DayPlan
import com.gmail.volkovskiyda.abit.core.domain.ScheduleRepository
import com.gmail.volkovskiyda.abit.core.domain.conflicts
import com.gmail.volkovskiyda.abit.core.domain.planFor
import com.gmail.volkovskiyda.abit.core.model.BREAK_MINUTES_RANGE
import com.gmail.volkovskiyda.abit.core.model.FOCUS_MINUTES_RANGE
import com.gmail.volkovskiyda.abit.core.model.LENGTH_STEP_MINUTES
import com.gmail.volkovskiyda.abit.core.model.Schedule
import com.gmail.volkovskiyda.abit.core.model.ScheduleId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime

data class EditorUiState(
    val draft: Schedule,
    val isNew: Boolean,
    /** The first overlap this draft would create with another enabled schedule, if any. */
    val conflict: Conflict? = null,
    val previewPlan: DayPlan? = null,
    val saved: Boolean = false,
) {
    val blockCount: Int get() = previewPlan?.blocks?.size ?: 0

    val focusCount: Int get() = previewPlan?.blocks?.count { it.kind == BlockKind.Focus } ?: 0

    /**
     * When the day actually stops — the end of the last block, which is **not** `draft.end` whenever
     * the rhythm does not divide the window evenly. Computed, so the caption is right even where the
     * mockups' numerals are not.
     */
    val lastBlockEnds: LocalTime? get() = previewPlan?.blocks?.lastOrNull()?.end

    val nameError: Boolean get() = draft.name.isBlank()
    val daysError: Boolean get() = draft.days.isEmpty()
    val hoursError: Boolean get() = draft.end <= draft.start
    val focusError: Boolean
        get() = draft.focusMinutes !in FOCUS_MINUTES_RANGE || draft.focusMinutes % LENGTH_STEP_MINUTES != 0
    val breakError: Boolean
        get() = draft.breakMinutes !in BREAK_MINUTES_RANGE || draft.breakMinutes % LENGTH_STEP_MINUTES != 0

    val canSave: Boolean get() = !nameError && !daysError && !hoursError && !focusError && !breakError
}

/**
 * The editor. Everything it shows about the schedule being written — how many blocks it produces,
 * when the last one ends, whether it overlaps another — is derived from item 01's planner rather than
 * stored, so the preview cannot drift from what the app will actually do.
 */
class ScheduleEditorViewModel(
    private val id: String?,
    private val repository: ScheduleRepository,
    private val clock: LocalClock,
) : ViewModel() {
    private val editorState = MutableStateFlow(EditorUiState(draft = blankSchedule(clock), isNew = id == null))

    val state: StateFlow<EditorUiState> = editorState.asStateFlow()

    init {
        viewModelScope.launch {
            val existing = id?.let { repository.findById(ScheduleId(it)) }
            editorState.value = EditorUiState(draft = existing ?: blankSchedule(clock), isNew = existing == null)
            refresh()
        }
    }

    /**
     * The whole form is one object, so the screen edits it as one rather than through a setter per
     * field. Every change re-derives the preview and the overlap, which is what keeps the editor's
     * caption honest while it is being typed into.
     */
    fun edit(transform: (Schedule) -> Schedule) {
        editorState.update { it.copy(draft = transform(it.draft)) }
        viewModelScope.launch { refresh() }
    }

    fun save() {
        if (!editorState.value.canSave) return
        viewModelScope.launch {
            repository.save(editorState.value.draft)
            editorState.update { it.copy(saved = true) }
        }
    }

    fun delete() {
        val existing = id ?: return
        viewModelScope.launch {
            repository.delete(ScheduleId(existing))
            editorState.update { it.copy(saved = true) }
        }
    }

    /**
     * Re-derives the preview and the overlap from the current draft plus everything else stored.
     *
     * The draft is read **inside** the update rather than before the suspending load, and that is
     * the whole point of the shape. A keystroke launches one of these, so several are in flight at
     * once and they finish in whatever order the database answers in. Reading the draft up front let
     * a slow one land last and overwrite the preview with a plan derived from a draft the user had
     * already typed past — and since only an edit starts a refresh, nothing corrected it until the
     * next keystroke. Taken from the state being written to, every ordering leaves a preview that
     * matches the draft beside it.
     *
     * The other schedules can be a moment stale without the same consequence: they change when
     * another device syncs, not when this one types, and the next refresh picks them up.
     */
    private suspend fun refresh() {
        val others = repository.observeSchedules().first()
        editorState.update { state ->
            val draft = state.draft
            val previewDate = draft.days.firstOrNull()?.let { day -> clock.today().nextOrSame(day) } ?: clock.today()
            val rest = others.filterNot { it.id == draft.id }
            state.copy(
                previewPlan = draft.copy(enabled = true).planFor(previewDate),
                conflict = (rest + draft).conflicts().firstOrNull { c -> c.first == draft.id || c.second == draft.id },
            )
        }
    }
}

private const val DEFAULT_FOCUS_MINUTES = 45
private const val DEFAULT_BREAK_MINUTES = 15
private const val NINE_AM_HOUR = 9
private const val SIX_PM_HOUR = 18
private const val DAYS_IN_WEEK = 7
private val WORKDAYS =
    setOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
    )

/** The schedule the "new schedule" button opens on: the design's own defaults. */
private fun blankSchedule(clock: LocalClock) =
    Schedule(
        id = ScheduleId("schedule-${clock.instant().toEpochMilliseconds()}"),
        name = "",
        enabled = true,
        days = WORKDAYS,
        start = LocalTime(NINE_AM_HOUR, 0),
        end = LocalTime(SIX_PM_HOUR, 0),
        focusMinutes = DEFAULT_FOCUS_MINUTES,
        breakMinutes = DEFAULT_BREAK_MINUTES,
        updatedAt = clock.instant(),
    )

/** The next date on or after this one that falls on [day] — so the preview is of a day that runs. */
private fun kotlinx.datetime.LocalDate.nextOrSame(day: DayOfWeek): kotlinx.datetime.LocalDate {
    var candidate = this
    repeat(DAYS_IN_WEEK) {
        if (candidate.dayOfWeek == day) return candidate
        candidate = kotlinx.datetime.LocalDate.fromEpochDays(candidate.toEpochDays() + 1)
    }
    return this
}
