package com.gmail.volkovskiyda.abit.core.designsystem.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.gmail.volkovskiyda.abit.core.domain.BlockKind

/** One of the three places the mode colour is allowed to appear. Uppercase, tracked out, small. */
@Composable
fun ModeLabel(
    stage: BlockKind?,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stage.label(),
        style = MaterialTheme.typography.labelSmall,
        color = stage.modeColor(),
        modifier = modifier,
    )
}

/** Null means off hours, which takes no accent at all. */
@Composable
fun BlockKind?.modeColor(): Color =
    when (this) {
        BlockKind.Focus -> MaterialTheme.colorScheme.primary
        BlockKind.Break -> MaterialTheme.colorScheme.tertiary
        null -> MaterialTheme.colorScheme.onSurfaceVariant
    }

fun BlockKind?.label(): String =
    when (this) {
        BlockKind.Focus -> "FOCUS"
        BlockKind.Break -> "BREAK"
        null -> "OFF HOURS"
    }
