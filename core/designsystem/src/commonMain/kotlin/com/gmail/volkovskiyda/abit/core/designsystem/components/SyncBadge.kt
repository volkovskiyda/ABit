package com.gmail.volkovskiyda.abit.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gmail.volkovskiyda.abit.core.domain.SyncState

/**
 * The cloud in the top bar. Filled means the schedules are on the server; an outline means they are
 * not and never will be until the user signs in; a dot means a write is in flight.
 *
 * Drawn rather than loaded so the same glyph works on Android, desktop and the web without an icon
 * dependency any of them would resolve differently.
 */
@Composable
fun SyncBadge(
    state: SyncState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val filled = state is SyncState.Idle
    val tint =
        when (state) {
            is SyncState.Failed -> MaterialTheme.colorScheme.error
            is SyncState.Idle -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.outline
        }
    Canvas(
        modifier
            .size(24.dp)
            .clickable(onClick = onClick)
            .semantics { contentDescription = state.describe() },
    ) {
        val w = size.width
        val h = size.height
        val cloud =
            Path().apply {
                moveTo(w * 0.28f, h * 0.70f)
                cubicTo(w * 0.05f, h * 0.70f, w * 0.05f, h * 0.40f, w * 0.30f, h * 0.40f)
                cubicTo(w * 0.34f, h * 0.18f, w * 0.68f, h * 0.18f, w * 0.72f, h * 0.42f)
                cubicTo(w * 0.96f, h * 0.44f, w * 0.96f, h * 0.70f, w * 0.74f, h * 0.70f)
                close()
            }
        if (filled) drawPath(cloud, tint) else drawPath(cloud, tint, style = Stroke(width = 1.5.dp.toPx()))
        if (state is SyncState.Syncing) {
            drawCircle(tint, radius = 2.5.dp.toPx(), center = Offset(w * 0.86f, h * 0.84f))
        }
    }
}

private fun SyncState.describe(): String =
    when (this) {
        SyncState.Unavailable -> "Sync unavailable in this build"
        SyncState.SignedOut -> "Signed out"
        SyncState.LocalOnly -> "On this device only"
        SyncState.Syncing -> "Syncing"
        is SyncState.Idle -> "Synced"
        is SyncState.Failed -> "Sync failed"
    }
