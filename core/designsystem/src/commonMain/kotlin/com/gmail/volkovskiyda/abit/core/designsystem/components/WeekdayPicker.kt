package com.gmail.volkovskiyda.abit.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.DayOfWeek

private val CIRCLE = 40.dp

val WORKDAYS: Set<DayOfWeek> =
    setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)

/**
 * Seven circles Mon…Sun plus the two presets. [conflicting] days get a 2 dp error ring: that is how
 * an overlap with another schedule is shown in place, without a dialog.
 */
@Composable
fun WeekdayPicker(
    selected: Set<DayOfWeek>,
    onSelectedChange: (Set<DayOfWeek>) -> Unit,
    modifier: Modifier = Modifier,
    conflicting: Set<DayOfWeek> = emptySet(),
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            DayOfWeek.entries.forEach { day ->
                val isSelected = day in selected
                val container =
                    if (isSelected) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .size(CIRCLE)
                            .background(container, CircleShape)
                            .then(
                                if (day in conflicting) {
                                    Modifier.border(2.dp, MaterialTheme.colorScheme.error, CircleShape)
                                } else {
                                    Modifier
                                },
                            ).clickable {
                                onSelectedChange(if (isSelected) selected - day else selected + day)
                            },
                ) {
                    Text(
                        text = day.name.take(1),
                        style = MaterialTheme.typography.labelSmall,
                        color =
                            if (isSelected) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selected == WORKDAYS,
                onClick = { onSelectedChange(WORKDAYS) },
                label = { Text("Workdays") },
            )
            FilterChip(
                selected = selected == DayOfWeek.entries.toSet(),
                onClick = { onSelectedChange(DayOfWeek.entries.toSet()) },
                label = { Text("Every day") },
            )
        }
    }
}
