package com.gmail.volkovskiyda.abit.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.gmail.volkovskiyda.abit.core.domain.BlockKind

/**
 * What a boundary looks like on the wrist: full-bleed mode colour, the mode word, when it ends, and
 * two actions. Launched by the chime notification's full-screen intent (item 08) and vibrating with
 * it.
 *
 * **Skip** writes a synced day override, so skipping here also silences the phone — which is the
 * point of the boundary being a shared fact rather than a per-device alarm.
 */
@Composable
fun WearChimeScreen(
    stage: BlockKind,
    untilLabel: String,
    thenLabel: String,
    onDismiss: () -> Unit,
    onSkipNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = if (stage == BlockKind.Focus) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
    Column(
        modifier = modifier.fillMaxSize().background(accent).padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = if (stage == BlockKind.Focus) "Focus" else "Break",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onPrimary,
        )
        Text(
            text = untilLabel,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimary,
        )
        Text(
            text = thenLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimary,
            textAlign = TextAlign.Center,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
            Button(onClick = onDismiss) { Text("OK") }
            Button(onClick = onSkipNext) { Text("Skip") }
        }
    }
}
