package com.gmail.volkovskiyda.abit.core.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gmail.volkovskiyda.abit.core.designsystem.hhmm
import kotlinx.datetime.LocalTime

/** Label left, time right in tabular numerals. Tapping opens the platform's own time picker. */
@Composable
fun TimeRow(
    label: String,
    time: LocalTime,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        TabularText(text = hhmm(time), style = MaterialTheme.typography.titleMedium)
    }
}
