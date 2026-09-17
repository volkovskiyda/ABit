package com.gmail.volkovskiyda.abit.ui.schedules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TimePickerDialogDefaults
import androidx.compose.material3.TimePickerDisplayMode
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gmail.volkovskiyda.abit.core.designsystem.components.LengthStepper
import com.gmail.volkovskiyda.abit.core.designsystem.components.TimeRow
import com.gmail.volkovskiyda.abit.core.designsystem.components.WeekdayPicker
import com.gmail.volkovskiyda.abit.core.designsystem.hhmm
import com.gmail.volkovskiyda.abit.core.designsystem.timeRange
import com.gmail.volkovskiyda.abit.core.model.BREAK_MINUTES_RANGE
import com.gmail.volkovskiyda.abit.core.model.FOCUS_MINUTES_RANGE
import com.gmail.volkovskiyda.abit.core.model.Schedule
import com.gmail.volkovskiyda.abit.feature.schedules.impl.EditorUiState
import com.gmail.volkovskiyda.abit.feature.schedules.impl.ScheduleEditorViewModel
import kotlinx.datetime.LocalTime

@Composable
fun ScheduleEditorScreen(
    viewModel: ScheduleEditorViewModel,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onDone() }
    ScheduleEditorContent(
        state = state,
        onEdit = viewModel::edit,
        onSave = viewModel::save,
        onDelete = viewModel::delete,
        onBack = onDone,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleEditorContent(
    state: EditorUiState,
    onEdit: ((Schedule) -> Schedule) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmingDelete by remember { mutableStateOf(false) }
    var editingTime by remember { mutableStateOf<TimeField?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(if (state.isNew) "New schedule" else "Edit schedule") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Cancel") } },
                actions = { TextButton(onClick = onSave, enabled = state.canSave) { Text("Save") } },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Section("NAME") {
                OutlinedTextField(
                    value = state.draft.name,
                    onValueChange = { name -> onEdit { it.copy(name = name) } },
                    singleLine = true,
                    isError = state.nameError,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Section("DAYS") {
                WeekdayPicker(
                    selected = state.draft.days,
                    onSelectedChange = { days -> onEdit { it.copy(days = days) } },
                    conflicting = state.conflict?.days.orEmpty(),
                )
            }

            Section("HOURS") {
                TimeRow(
                    label = "Starts",
                    time = state.draft.start,
                    onClick = { editingTime = TimeField.Start },
                )
                TimeRow(
                    label = "Ends",
                    time = state.draft.end,
                    onClick = { editingTime = TimeField.End },
                )
                val conflict = state.conflict
                if (conflict != null) {
                    Text(
                        text = "Overlaps another schedule, ${timeRange(conflict.from, conflict.to)}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Section("RHYTHM") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LengthStepper(
                        minutes = state.draft.focusMinutes,
                        range = FOCUS_MINUTES_RANGE,
                        onMinutesChange = { minutes -> onEdit { it.copy(focusMinutes = minutes) } },
                    )
                    LengthStepper(
                        minutes = state.draft.breakMinutes,
                        range = BREAK_MINUTES_RANGE,
                        onMinutesChange = { minutes -> onEdit { it.copy(breakMinutes = minutes) } },
                    )
                }
                // Computed, not transcribed: the caption is right even where the mockup's numerals
                // are illustrative.
                Text(
                    text = state.rhythmCaption(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (!state.isNew) {
                TextButton(
                    onClick = { confirmingDelete = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete schedule") }
            }
        }
    }

    // Keyed by the field: the two rows share one call site, so without it the picker would open
    // on the hour the other row was last set to.
    editingTime?.let { field ->
        key(field) {
            TimeFieldDialog(
                field = field,
                time = field.of(state.draft),
                onDismiss = { editingTime = null },
                onConfirm = { picked ->
                    editingTime = null
                    onEdit { field.set(it, picked) }
                },
            )
        }
    }

    if (confirmingDelete) {
        // The error colour appears here and nowhere else — never in a list row.
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("Delete ${state.draft.name}?") },
            text = { Text("It stops chiming on every device. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingDelete = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text("Cancel") } },
        )
    }
}

/** Which of the two rows the picker is open on, and how it reads and writes the draft. */
private enum class TimeField(
    val title: String,
) {
    Start("Starts at") {
        override fun of(schedule: Schedule): LocalTime = schedule.start

        override fun set(
            schedule: Schedule,
            time: LocalTime,
        ): Schedule = schedule.copy(start = time)
    },
    End("Ends at") {
        override fun of(schedule: Schedule): LocalTime = schedule.end

        override fun set(
            schedule: Schedule,
            time: LocalTime,
        ): Schedule = schedule.copy(end = time)
    },
    ;

    abstract fun of(schedule: Schedule): LocalTime

    abstract fun set(
        schedule: Schedule,
        time: LocalTime,
    ): Schedule
}

/**
 * The platform's own time picker, forced to 24 hours: [hhmm] is 24-hour on every surface, and a
 * picker that offered an AM/PM toggle would be the one place in the app that disagreed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeFieldDialog(
    field: TimeField,
    time: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
) {
    val picker = rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute, is24Hour = true)
    // Boolean rather than TimePickerDisplayMode: the mode is an inline value class, which
    // rememberSaveable has no saver for and would throw on.
    var typing by rememberSaveable { mutableStateOf(false) }

    TimePickerDialog(
        onDismissRequest = onDismiss,
        title = { Text(field.title) },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime(picker.hour, picker.minute)) }) { Text("Set") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        modeToggleButton = {
            TimePickerDialogDefaults.DisplayModeToggle(
                onDisplayModeChange = { typing = !typing },
                displayMode = if (typing) TimePickerDisplayMode.Input else TimePickerDisplayMode.Picker,
            )
        },
    ) {
        if (typing) TimeInput(state = picker) else TimePicker(state = picker)
    }
}

@Composable
private fun Section(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

internal fun EditorUiState.rhythmCaption(): String {
    val ends: LocalTime? = lastBlockEnds
    return if (ends == null) {
        "Nothing runs on the chosen days"
    } else {
        "$blockCount blocks · $focusCount focus · last block ends ${hhmm(ends)}"
    }
}
