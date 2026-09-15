package com.gmail.volkovskiyda.abit.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.gmail.volkovskiyda.abit.core.designsystem.timeRange
import com.gmail.volkovskiyda.abit.core.model.Schedule
import kotlinx.datetime.DayOfWeek

/** A disabled schedule renders at 38 % on-surface, switch off, no strike-through. */
private const val DISABLED_ALPHA = 0.38f

/**
 * One row of the Schedules list. The whole card taps into the editor; only the switch is separately
 * hittable, which is why [onToggle] is its own callback rather than part of [onClick].
 */
@Composable
fun ScheduleCard(
    schedule: Schedule,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    overlapLabel: String? = null,
    conflictingDays: Set<DayOfWeek> = emptySet(),
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium)
                .clickable(onClick = onClick)
                .padding(16.dp)
                .alpha(if (schedule.enabled) 1f else DISABLED_ALPHA),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(schedule.name, style = MaterialTheme.typography.titleMedium)
                TabularText(
                    text = timeRange(schedule.start, schedule.end),
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                )
            }
            Switch(checked = schedule.enabled, onCheckedChange = onToggle)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DayOfWeek.entries.forEach { day ->
                val on = day in schedule.days
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .size(28.dp)
                            .background(
                                if (on) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                CircleShape,
                            ).then(
                                if (day in conflictingDays) {
                                    Modifier.border(2.dp, MaterialTheme.colorScheme.error, CircleShape)
                                } else {
                                    Modifier
                                },
                            ),
                ) {
                    Text(
                        text = day.name.take(1),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (on) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Text(
            text = "${schedule.focusMinutes} min focus · ${schedule.breakMinutes} min break",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (overlapLabel != null) {
            AbitChip(
                text = overlapLabel,
                container = MaterialTheme.colorScheme.errorContainer,
                content = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}
