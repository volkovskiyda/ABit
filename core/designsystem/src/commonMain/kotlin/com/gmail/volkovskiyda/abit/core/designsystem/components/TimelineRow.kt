package com.gmail.volkovskiyda.abit.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.gmail.volkovskiyda.abit.core.designsystem.timeRange
import com.gmail.volkovskiyda.abit.core.domain.Block
import com.gmail.volkovskiyda.abit.core.domain.BlockKind

private const val PAST_ALPHA = 0.6f
private val DOT_SIZE = 12.dp

enum class TimelinePosition { Past, Current, Future }

/**
 * One block in "Rest of today". The dot says where the block sits relative to now — filled behind,
 * ringed at, hollow ahead — and the mode colour is carried by the dot alone, not by the text.
 */
@Composable
fun TimelineRow(
    block: Block,
    position: TimelinePosition,
    modifier: Modifier = Modifier,
    skipped: Boolean = false,
) {
    val color = block.kind.modeColor()
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp).alpha(if (position == TimelinePosition.Past) PAST_ALPHA else 1f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(DOT_SIZE).drawBehind {
                val radius = size.minDimension / 2f
                when (position) {
                    TimelinePosition.Past -> drawCircle(color, radius = radius)
                    TimelinePosition.Current -> drawCircle(color, radius = radius - 1.dp.toPx(), style = Stroke(width = 2.dp.toPx()))
                    TimelinePosition.Future -> drawCircle(color, radius = radius - 1.dp.toPx(), style = Stroke(width = 1.dp.toPx()))
                }
            },
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = if (block.kind == BlockKind.Focus) "Focus" else "Break",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.width(72.dp),
        )
        TabularText(
            text = timeRange(block.start, block.end),
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            modifier = Modifier.weight(1f),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (skipped) AbitChip(text = "skipped")
            if (position == TimelinePosition.Current) AbitChip(text = "now", accent = true)
        }
    }
}
