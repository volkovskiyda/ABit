package com.gmail.volkovskiyda.abit.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gmail.volkovskiyda.abit.core.model.LENGTH_STEP_MINUTES

/**
 * The pill `− value +`. Steps by [LENGTH_STEP_MINUTES] and clamps to [range], so the editor cannot
 * produce a schedule the planner would have to defend itself against.
 */
@Composable
fun LengthStepper(
    minutes: Int,
    range: IntRange,
    onMinutesChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { onMinutesChange((minutes - LENGTH_STEP_MINUTES).coerceIn(range)) },
            enabled = minutes > range.first,
        ) {
            Text("−", style = MaterialTheme.typography.titleMedium)
        }
        TabularText(
            text = "$minutes min",
            style = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
            modifier = Modifier.widthIn(min = 72.dp),
        )
        IconButton(
            onClick = { onMinutesChange((minutes + LENGTH_STEP_MINUTES).coerceIn(range)) },
            enabled = minutes < range.last,
        ) {
            Text("+", style = MaterialTheme.typography.titleMedium)
        }
    }
}
