package com.gmail.volkovskiyda.abit.web.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gmail.volkovskiyda.abit.core.designsystem.components.ScheduleCard
import com.gmail.volkovskiyda.abit.feature.schedules.impl.SchedulesViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * The list and the editor side by side, which is the tablet layout the design specifies for the web.
 * The editor itself is item 12's gap: it is a pane placeholder until the web time picker exists.
 */
@Composable
fun WebSchedulesScreen(modifier: Modifier = Modifier) {
    val viewModel: SchedulesViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<String?>(null) }

    Row(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(
            modifier = Modifier.weight(2f).widthIn(max = CONTENT_MAX_WIDTH).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Schedules", style = MaterialTheme.typography.headlineMedium)
            state.schedules.forEach { schedule ->
                ScheduleCard(
                    schedule = schedule,
                    onClick = { selected = schedule.id.value },
                    onToggle = { viewModel.toggle(schedule.id, it) },
                    conflictingDays = state.conflictsFor(schedule.id).flatMap { it.days }.toSet(),
                )
            }
            if (state.schedules.isEmpty()) {
                Text(
                    "No schedules yet. Add one on your phone, or here once the web editor lands.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Column(Modifier.weight(3f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val schedule = state.schedules.firstOrNull { it.id.value == selected }
            if (schedule == null) {
                Text(
                    "Select a schedule to see it here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(schedule.name, style = MaterialTheme.typography.headlineMedium)
                Text(
                    "${schedule.focusMinutes} min focus · ${schedule.breakMinutes} min break",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(onClick = { viewModel.toggle(schedule.id, !schedule.enabled) }, modifier = Modifier.fillMaxWidth(0.4f)) {
                    Text(if (schedule.enabled) "Turn off" else "Turn on")
                }
            }
        }
    }
}
