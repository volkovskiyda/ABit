package com.gmail.volkovskiyda.abit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gmail.volkovskiyda.abit.core.designsystem.components.ScheduleCard
import com.gmail.volkovskiyda.abit.core.designsystem.timeRange
import com.gmail.volkovskiyda.abit.core.model.ScheduleId
import com.gmail.volkovskiyda.abit.feature.schedules.impl.SchedulesUiState

/**
 * The list–detail layout again, on the Mac. A third copy of the composition rather than a shared
 * screen — the per-platform-UI rule, narrowed in item 06 to *screens* rather than components, is what
 * makes that the intended shape: the ring, the card and the row underneath are all shared.
 */
@Composable
fun SchedulesWindowContent(
    state: SchedulesUiState,
    onToggle: (ScheduleId, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by remember { mutableStateOf<String?>(null) }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Row(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(
                modifier = Modifier.weight(2f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Schedules", style = MaterialTheme.typography.headlineMedium)
                state.schedules.forEach { schedule ->
                    ScheduleCard(
                        schedule = schedule,
                        onClick = { selected = schedule.id.value },
                        onToggle = { onToggle(schedule.id, it) },
                        conflictingDays = state.conflictsFor(schedule.id).flatMap { it.days }.toSet(),
                    )
                }
                if (state.schedules.isEmpty()) {
                    Text(
                        "No schedules yet. Add one on your phone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Column(Modifier.weight(3f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val schedule = state.schedules.firstOrNull { it.id.value == selected }
                if (schedule == null) {
                    Text(
                        "Select a schedule to see it here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(schedule.name, style = MaterialTheme.typography.headlineMedium)
                    Text(timeRange(schedule.start, schedule.end), style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${schedule.focusMinutes} min focus · ${schedule.breakMinutes} min break",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
