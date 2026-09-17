package com.gmail.volkovskiyda.abit.wear.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.requestFocusOnHierarchyActive
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
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

/**
 * The *most* the ring may be, not what it is. [SessionRing] sizes itself exactly, so a diameter
 * wider than the column renders as an ellipse — the width gets clamped by the parent while the
 * height, inside a vertical scroll, does not. Measuring the column and taking the smaller of the two
 * is what keeps it a circle on every watch, whatever the scaffold's round-screen padding leaves.
 */
private val RING_MAX_DIAMETER = 180.dp

@Composable
fun WearTodayScreen(
    viewModel: TodayViewModel,
    onOpenSchedules: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    WearTodayContent(state = state, onOpenSchedules = onOpenSchedules, modifier = modifier)
}

/**
 * **No glow, no pulse, no animation.** The brief says none, and a stray glow had to be removed from
 * these very screens during the design review — this thing is on a wrist all day.
 */
@Composable
fun WearTodayContent(
    state: TodayUiState,
    onOpenSchedules: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = state.today
    // The ring is taller than the dial once the buttons are under it, so this column actually
    // scrolls. It did not before: the layout said "below the fold, reachable by rotary or scroll"
    // while being a plain Column, so the button was simply clipped off the bottom of the watch and
    // nothing on the round screen could reach it.
    val scrollState = rememberScrollState()
    // rotaryScrollable needs the focus the crown's events are delivered to, and
    // requestFocusOnHierarchyActive is what hands it over — the older rememberActiveFocusRequester
    // that reads more naturally here is deprecated in Wear Compose 1.6.
    val focusRequester = remember { FocusRequester() }
    ScreenScaffold(modifier = modifier, scrollState = scrollState, timeText = { TimeText() }) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .requestFocusOnHierarchyActive()
                    .focusRequester(focusRequester)
                    .rotaryScrollable(RotaryScrollableDefaults.behavior(scrollState), focusRequester)
                    .padding(padding)
                    .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BoxWithConstraints {
                SessionRing(
                    arcs = if (today is TodayState.Running) ringArcs(today.session, today.sessionRemaining) else RingArcs.Empty,
                    stage = (today as? TodayState.Running)?.stage ?: BlockKind.Focus,
                    diameter = minOf(maxWidth, RING_MAX_DIAMETER),
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
            }

            // Below the fold on purpose: reachable by rotary or scroll, never in the way of the ring.
            //
            // The only control here, and the only way into the schedules list — which is also the
            // only way to the sign-in card and the permission rows at the top of it. Without it the
            // watch's second destination was registered and unreachable, so the watch could not be
            // signed in at all, and an anonymous watch is one whose schedules never arrive.
            //
            // Skipping the day is deliberately *not* offered here. It belongs on a screen where the
            // consequence is legible; on a wrist it is one stray tap between a working day and a
            // silent one.
            Button(onClick = onOpenSchedules) {
                Text("Schedules")
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
        is TodayState.Skipped -> "SKIPPED"
        is TodayState.OffHours -> "OFF HOURS"
    }

private fun TodayState.headline(): String =
    when (this) {
        is TodayState.Running -> countdown(stageRemaining)
        is TodayState.Skipped -> "—"
        is TodayState.OffHours -> next?.at?.let(::hhmm) ?: "—"
    }

private fun TodayState.caption(): String =
    when (this) {
        is TodayState.Running -> "${hhmm(nextBoundary)} · ends ${hhmm(session.end)}"
        is TodayState.Skipped -> "until tomorrow"
        is TodayState.OffHours -> next?.scheduleName ?: "no schedule"
    }
