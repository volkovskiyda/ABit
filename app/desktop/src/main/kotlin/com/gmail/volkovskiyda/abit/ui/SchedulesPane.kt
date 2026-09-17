package com.gmail.volkovskiyda.abit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gmail.volkovskiyda.abit.core.designsystem.components.ScheduleCard
import com.gmail.volkovskiyda.abit.core.model.ScheduleId
import com.gmail.volkovskiyda.abit.feature.schedules.impl.SchedulesUiState

/**
 * The popover's second pane, not a window of its own.
 *
 * The Mac has no editor — a schedule is written on the phone — so the list is the whole screen and
 * the detail column the old window carried said nothing [ScheduleCard] does not already show. That
 * is what lets the pane live at the popover's width: the card is the detail.
 *
 * A third copy of the list rather than a shared screen — the per-platform-UI rule, narrowed in item
 * 06 to *screens* rather than components, is what makes that the intended shape: the card itself is
 * shared.
 */
@Composable
fun SchedulesPaneContent(
    state: SchedulesUiState,
    onToggle: (ScheduleId, Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PopoverSurface(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ Today") }
            Text(
                text = "Schedules",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.schedules.forEach { schedule ->
                ScheduleCard(
                    schedule = schedule,
                    // Nothing to open: the card is already the detail, and the switch is its own
                    // hit target, so a tap on the rest of it has nowhere to go.
                    onClick = {},
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
    }
}
