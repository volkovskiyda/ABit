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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
                TimeRow(label = "Starts", time = state.draft.start, onClick = {})
                TimeRow(label = "Ends", time = state.draft.end, onClick = {})
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
