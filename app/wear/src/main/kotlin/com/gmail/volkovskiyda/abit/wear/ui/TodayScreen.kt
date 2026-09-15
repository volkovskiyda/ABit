package com.gmail.volkovskiyda.abit.wear.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import com.gmail.volkovskiyda.abit.core.designsystem.RingArcs
import com.gmail.volkovskiyda.abit.core.designsystem.components.SessionRing
import com.gmail.volkovskiyda.abit.core.designsystem.countdown
import com.gmail.volkovskiyda.abit.core.designsystem.hhmm
import com.gmail.volkovskiyda.abit.core.designsystem.ringArcs
import com.gmail.volkovskiyda.abit.core.domain.BlockKind
import com.gmail.volkovskiyda.abit.core.domain.TodayState
import com.gmail.volkovskiyda.abit.feature.today.impl.TodayUiState
import com.gmail.volkovskiyda.abit.feature.today.impl.TodayViewModel

/** The brief: the ring hugs the bezel at a 6 dp stroke, everything inside an 8 % inset. */
private val WEAR_RING_STROKE = 6.dp
private val RING_DIAMETER = 180.dp

@Composable
fun WearTodayScreen(
    viewModel: TodayViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    WearTodayContent(state = state, onPauseToday = { viewModel.pauseToday(it) }, modifier = modifier)
}

/**
 * **No glow, no pulse, no animation.** The brief says none, and a stray glow had to be removed from
 * these very screens during the design review — this thing is on a wrist all day.
 */
@Composable
fun WearTodayContent(
    state: TodayUiState,
    onPauseToday: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = state.today
    ScreenScaffold(modifier = modifier, timeText = { TimeText() }) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            SessionRing(
                arcs = if (today is TodayState.Running) ringArcs(today.session, today.remaining) else RingArcs.Empty,
                stage = (today as? TodayState.Running)?.stage ?: BlockKind.Focus,
                diameter = RING_DIAMETER,
                strokeWidth = WEAR_RING_STROKE,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = today.mode(),
                        style = MaterialTheme.typography.labelSmall,
                        color = today.modeColor(),
                    )
                    Text(
                        text = today.headline(),
                        // 48 sp, never wider than 60 % of the dial's inner width — the brief's rule.
                        style = MaterialTheme.typography.displaySmall.copy(fontSize = 48.sp, fontFeatureSettings = "tnum"),
                    )
                    Text(
                        text = today.caption(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // Below the fold on purpose: reachable by rotary or scroll, never in the way of the ring.
            Button(onClick = { onPauseToday(today !is TodayState.Paused) }) {
                Text(if (today is TodayState.Paused) "Resume today" else "Pause today")
            }
        }
    }
}

@Composable
private fun TodayState.modeColor() =
    when (this) {
        is TodayState.Running -> if (stage == BlockKind.Focus) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

private fun TodayState.mode(): String =
    when (this) {
        is TodayState.Running -> if (stage == BlockKind.Focus) "FOCUS" else "BREAK"
        is TodayState.Paused -> "PAUSED"
        is TodayState.OffHours -> "OFF HOURS"
    }

private fun TodayState.headline(): String =
    when (this) {
        is TodayState.Running -> countdown(remaining)
        is TodayState.Paused -> "—"
        is TodayState.OffHours -> next?.at?.let(::hhmm) ?: "—"
    }

private fun TodayState.caption(): String =
    when (this) {
        is TodayState.Running -> "${hhmm(nextBoundary)} · ends ${hhmm(session.end)}"
        is TodayState.Paused -> "until tomorrow"
        is TodayState.OffHours -> next?.scheduleName ?: "no schedule"
    }
