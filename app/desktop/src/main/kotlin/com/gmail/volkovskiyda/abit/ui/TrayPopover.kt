package com.gmail.volkovskiyda.abit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gmail.volkovskiyda.abit.core.designsystem.RingArcs
import com.gmail.volkovskiyda.abit.core.designsystem.components.CountdownText
import com.gmail.volkovskiyda.abit.core.designsystem.components.ModeLabel
import com.gmail.volkovskiyda.abit.core.designsystem.components.SessionRing
import com.gmail.volkovskiyda.abit.core.designsystem.components.TimelinePosition
import com.gmail.volkovskiyda.abit.core.designsystem.components.TimelineRow
import com.gmail.volkovskiyda.abit.core.designsystem.dayLabel
import com.gmail.volkovskiyda.abit.core.designsystem.hhmm
import com.gmail.volkovskiyda.abit.core.designsystem.ringArcs
import com.gmail.volkovskiyda.abit.core.domain.BlockKind
import com.gmail.volkovskiyda.abit.core.domain.TodayState
import com.gmail.volkovskiyda.abit.feature.today.impl.TodayUiState

/** The design's popover: 320 × 440 under the menu-bar item, with a 4 dp ring. */
private val POPOVER_RING_STROKE = 4.dp
private val POPOVER_RING_DIAMETER = 160.dp

/** How many of today's blocks fit under the ring before the popover has to scroll. */
private const val TIMELINE_ROWS = 4

/**
 * Stateless on purpose: the whole popover renders from one [TodayUiState] plus callbacks, so the
 * desktop UI test can drive it through Skiko with no Koin graph and no window.
 *
 * It draws its own rounded background and hairline border because the window is `undecorated` and
 * `transparent` — that pair is what makes this feel like a popover rather than a small window.
 */
@Composable
fun TrayPopoverContent(
    state: TodayUiState,
    chimeOnThisMac: Boolean,
    onPauseToday: (Boolean) -> Unit,
    onSkipNext: () -> Unit,
    onChimeOnThisMac: (Boolean) -> Unit,
    onOpenSchedules: () -> Unit,
    onQuit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = state.today
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SessionRing(
            arcs = if (today is TodayState.Running) ringArcs(today.session, today.remaining) else RingArcs.Empty,
            stage = (today as? TodayState.Running)?.stage ?: BlockKind.Focus,
            diameter = POPOVER_RING_DIAMETER,
            strokeWidth = POPOVER_RING_STROKE,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                ModeLabel((today as? TodayState.Running)?.stage)
                when (today) {
                    is TodayState.Running -> {
                        CountdownText(today.remaining, style = MaterialTheme.typography.headlineMedium)
                    }

                    is TodayState.Paused -> {
                        Text("Paused", style = MaterialTheme.typography.headlineMedium)
                    }

                    is TodayState.OffHours -> {
                        Text(today.next?.at?.let(::hhmm) ?: "—", style = MaterialTheme.typography.headlineMedium)
                    }
                }
                Text(
                    text = today.caption(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when (today) {
                is TodayState.Running -> {
                    FilledTonalButton(onClick = { onPauseToday(true) }) { Text("Pause today") }
                    OutlinedButton(onClick = onSkipNext) { Text("Skip next") }
                }

                is TodayState.Paused -> {
                    FilledTonalButton(onClick = { onPauseToday(false) }) { Text("Resume today") }
                }

                is TodayState.OffHours -> {
                    FilledTonalButton(onClick = { onPauseToday(true) }) { Text("Pause tomorrow") }
                }
            }
        }

        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
            val plan = today.plan()
            if (plan.sessions.isNotEmpty()) {
                Text("Rest of today", style = MaterialTheme.typography.titleMedium)
                val now = (today as? TodayState.Running)?.nextBoundary
                plan.blocks.filter { now == null || it.end >= now }.take(TIMELINE_ROWS).forEach { block ->
                    TimelineRow(
                        block = block,
                        position = if (block.end == now) TimelinePosition.Current else TimelinePosition.Future,
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Chime on this Mac", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Switch(checked = chimeOnThisMac, onCheckedChange = onChimeOnThisMac)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onOpenSchedules) { Text("Schedules…") }
            TextButton(onClick = onQuit) { Text("Quit") }
        }
    }
}

internal fun TodayState.plan() =
    when (this) {
        is TodayState.Running -> plan
        is TodayState.Paused -> plan
        is TodayState.OffHours -> today
    }

private fun TodayState.caption(): String =
    when (this) {
        is TodayState.Running -> "ends ${hhmm(session.end)}"
        is TodayState.Paused -> "Paused until ${dayLabel(resumesOn)}"
        is TodayState.OffHours -> next?.let { "${it.scheduleName}, ${hhmm(it.at)}" } ?: "No schedule this week"
    }
