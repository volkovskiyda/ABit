package com.gmail.volkovskiyda.abit.ui.schedules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gmail.volkovskiyda.abit.core.designsystem.timeRange
import com.gmail.volkovskiyda.abit.core.domain.Conflict
import com.gmail.volkovskiyda.abit.core.model.Schedule
import com.gmail.volkovskiyda.abit.core.model.ScheduleId

/**
 * Which of two overlapping schedules stays on. Reached from the Schedules list and, when an overlap
 * arrives by sync, from Today on the next launch (decision 7A) — the same composable both times.
 *
 * Resolving switches the loser **off entirely**, not for that day, which is what the copy promises.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConflictSheet(
    conflict: Conflict,
    schedules: List<Schedule>,
    onKeep: (keep: ScheduleId, disable: ScheduleId) -> Unit,
    onEditHours: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Two schedules overlap", style = MaterialTheme.typography.headlineMedium)
            Text(
                text =
                    "They both want ${timeRange(conflict.from, conflict.to)}. Only one schedule can run a day, " +
                        "so choose which stays on.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            listOf(conflict.first, conflict.second).forEach { id ->
                val schedule = schedules.firstOrNull { it.id == id } ?: return@forEach
                val other = if (id == conflict.first) conflict.second else conflict.first
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium)
                            .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(schedule.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = timeRange(schedule.start, schedule.end),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = { onKeep(id, other) }) { Text("Keep ${schedule.name}") }
                }
            }

            Text(
                text = "The other schedule is switched off. You can change its hours later.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = { onEditHours(conflict.second.value) }) { Text("Edit hours instead") }
        }
    }
}
