package com.gmail.volkovskiyda.abit.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** The small tonal pill the design uses for "now", "skipped", "Allowed" and the overlap hint. */
@Composable
fun AbitChip(
    text: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    container: Color = if (accent) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
    content: Color = if (accent) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = content,
        modifier = modifier.background(container, CircleShape).padding(horizontal = 8.dp, vertical = 4.dp),
    )
}
