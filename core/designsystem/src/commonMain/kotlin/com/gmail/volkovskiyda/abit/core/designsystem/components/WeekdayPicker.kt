package com.gmail.volkovskiyda.abit.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.datetime.DayOfWeek

private val CIRCLE = 40.dp
private val CONFLICT_RING = 2.dp
private val IDLE_RING = 1.dp

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
                DayCircle(
                    day = day,
                    selected = day in selected,
                    conflicting = day in conflicting,
                    onSelectedChange = { isOn -> onSelectedChange(if (isOn) selected + day else selected - day) },
                )
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

/**
 * On is `secondary` on `onSecondary`, off is a bare container behind an outline. The pair
 * `secondaryContainer` / `surfaceContainerHigh` the design named reads as one colour in the light
 * palette — #DCE3F7 against #E4E8F2 — so a selected day was not visibly selected.
 *
 * The circle is clipped **before** the toggle, which is what keeps the ripple inside it: an
 * indication drawn under a bare `clickable` is bounded by the node's rectangle, not by the shape the
 * background happens to be painted in.
 */
@Composable
private fun DayCircle(
    day: DayOfWeek,
    selected: Boolean,
    conflicting: Boolean,
    onSelectedChange: (Boolean) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val ring = if (conflicting) CONFLICT_RING else IDLE_RING
    val ringColor =
        when {
            conflicting -> colors.error
            selected -> Color.Transparent
            else -> colors.outlineVariant
        }
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(CIRCLE)
                .clip(CircleShape)
                .background(if (selected) colors.secondary else colors.surfaceContainerHigh)
                .border(ring, ringColor, CircleShape)
                .toggleable(value = selected, role = Role.Checkbox, onValueChange = onSelectedChange),
    ) {
        Text(
            text = day.name.take(1),
            style = MaterialTheme.typography.titleSmall,
            color = if (selected) colors.onSecondary else colors.onSurfaceVariant,
        )
    }
}
