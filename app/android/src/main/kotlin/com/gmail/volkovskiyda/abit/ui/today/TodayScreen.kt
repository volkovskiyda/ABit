package com.gmail.volkovskiyda.abit.ui.today

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gmail.volkovskiyda.abit.core.designsystem.RingArcs
import com.gmail.volkovskiyda.abit.core.designsystem.components.AbitChip
import com.gmail.volkovskiyda.abit.core.designsystem.components.CountdownText
import com.gmail.volkovskiyda.abit.core.designsystem.components.ModeLabel
import com.gmail.volkovskiyda.abit.core.designsystem.components.SessionRing
import com.gmail.volkovskiyda.abit.core.designsystem.components.SyncBadge
import com.gmail.volkovskiyda.abit.core.designsystem.components.TabularText
import com.gmail.volkovskiyda.abit.core.designsystem.components.TimelinePosition
import com.gmail.volkovskiyda.abit.core.designsystem.components.TimelineRow
import com.gmail.volkovskiyda.abit.core.designsystem.dayLabel
import com.gmail.volkovskiyda.abit.core.designsystem.hhmm
import com.gmail.volkovskiyda.abit.core.designsystem.hhmmss
import com.gmail.volkovskiyda.abit.core.designsystem.ringArcs
import com.gmail.volkovskiyda.abit.core.designsystem.sessionCaption
import com.gmail.volkovskiyda.abit.core.domain.BlockKind
import com.gmail.volkovskiyda.abit.core.domain.TodayState
import com.gmail.volkovskiyda.abit.feature.today.impl.TodayUiState
import com.gmail.volkovskiyda.abit.feature.today.impl.TodayViewModel
import com.gmail.volkovskiyda.abit.ui.isWideWindow
import kotlinx.datetime.LocalTime

/** The design's content ceiling: 1200 dp, so a desktop-width browser does not stretch a line of text. */
internal val CONTENT_MAX_WIDTH = 1200.dp

/**
 * The app's home. Three states in one screen — Focus, Break and Off hours — because they answer the
 * same three questions in the same order and only the accent and the actions differ.
 *
 * There is no start or stop control here, and there is not one anywhere else either: the schedule is
 * what starts things. The only intervention is Skip today.
 */
@Composable
fun TodayScreen(
    viewModel: TodayViewModel,
    onOpenSignIn: () -> Unit,
    onOpenConflict: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TodayContent(
        state = state,
        onSkipToday = { viewModel.skipToday(it) },
        onSkipTomorrow = { viewModel.skipTomorrow() },
        onOpenSignIn = onOpenSignIn,
        onOpenConflict = onOpenConflict,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayContent(
    state: TodayUiState,
    onSkipToday: (Boolean) -> Unit,
    onSkipTomorrow: () -> Unit,
    onOpenSignIn: () -> Unit,
    onOpenConflict: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        // Zero content insets: the NavigationSuiteScaffold outside this one already owns the bottom
        // edge, and a second Scaffold applying the same inset pushes its own content — the FAB most
        // visibly — under the navigation bar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Today", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            text = state.subtitle(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = { SyncBadge(state = state.syncState, onClick = onOpenSignIn) },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val conflict = state.unresolvedConflict
            if (conflict != null) {
                ConflictBanner(
                    onClick = { onOpenConflict(conflict.first.value, conflict.second.value) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(8.dp))
            if (isWideWindow()) {
                // The design's tablet layout: ring and actions on the left, the day on the right, at
                // roughly 2 : 3. Same composables, one row instead of a column.
                Row(
                    modifier = Modifier.fillMaxWidth().widthIn(max = CONTENT_MAX_WIDTH),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Column(
                        modifier = Modifier.weight(2f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        TodayRing(state.today)
                        WallClock(state.now)
                        TodayActions(state.today, onSkipToday, onSkipTomorrow)
                    }
                    Column(Modifier.weight(3f)) { RestOfToday(state.today, state.now) }
                }
            } else {
                TodayRing(state.today)
                WallClock(state.now)
                TodayActions(state.today, onSkipToday, onSkipTomorrow)
                RestOfToday(state.today, state.now)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ConflictBanner(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(onClick = onClick, modifier = modifier) {
        Text("Two schedules overlap — choose which stays on")
    }
}

@Composable
private fun TodayRing(today: TodayState) {
    val stage = today.stage()
    val arcs =
        when (today) {
            is TodayState.Running -> ringArcs(today.session, today.sessionRemaining)

            // Skipped and off hours draw the bare track: no accent at all, which is the design's rule.
            else -> RingArcs.Empty
        }
    SessionRing(arcs = arcs, stage = stage ?: BlockKind.Focus) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ModeLabel(stage)
            when (today) {
                is TodayState.Running -> {
                    CountdownText(today.stageRemaining)
                }

                is TodayState.Skipped -> {
                    Text("Skipped", style = MaterialTheme.typography.headlineMedium)
                }

                is TodayState.OffHours -> {
                    Text(
                        text = today.next?.at?.let(::hhmm) ?: "—",
                        style = MaterialTheme.typography.headlineMedium,
                    )
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
}

/**
 * The wall clock, between the ring and the day's one intervention.
 *
 * With the seconds, because the countdown inside the ring above it moves every second: a clock beside
 * a running one that only changed on the minute would read as the stale half of the pair. It costs no
 * ticker of its own — `TodayUiState.now` is already re-read every second to drive that countdown —
 * and it is tabular for the same reason the countdown is, so the digits do not jitter the row.
 */
@Composable
private fun WallClock(now: LocalTime) {
    TabularText(
        text = hhmmss(now),
        style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
    )
}

@Composable
private fun TodayActions(
    today: TodayState,
    onSkipToday: (Boolean) -> Unit,
    onSkipTomorrow: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        when (today) {
            is TodayState.Running -> {
                FilledTonalButton(onClick = { onSkipToday(true) }) { Text("Skip today") }
            }

            is TodayState.Skipped -> {
                Button(
                    onClick = { onSkipToday(false) },
                    colors = ButtonDefaults.buttonColors(),
                ) { Text("Resume today") }
            }

            is TodayState.OffHours -> {
                FilledTonalButton(onClick = onSkipTomorrow) { Text("Skip tomorrow") }
            }
        }
    }
}

/**
 * The day's blocks, with the ones already behind [now] folded into a single row. A schedule that
 * runs 09:00 to 18:00 at a 45/15 rhythm is eighteen blocks, and by the afternoon most of them are
 * history the user has to scroll past to reach the part they opened the app for.
 */
@Composable
private fun RestOfToday(
    today: TodayState,
    now: LocalTime,
) {
    val plan =
        when (today) {
            is TodayState.Running -> today.plan
            is TodayState.Skipped -> today.plan
            is TodayState.OffHours -> today.today
        }
    if (plan.sessions.isEmpty()) return

    val past = plan.blocks.filter { it.end <= now }
    val ahead = plan.blocks.drop(past.size)
    // Once the day is over there is no "rest" to collapse *towards*, so the group opens itself
    // rather than leaving the section looking empty.
    var expanded by rememberSaveable(ahead.isEmpty()) { mutableStateOf(ahead.isEmpty()) }

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (ahead.isEmpty()) "Earlier today" else "Rest of today",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            if (today is TodayState.Running) {
                AbitChip(text = "Session ${today.sessionNumber} of ${today.sessionCount}")
            }
        }
        if (past.isNotEmpty()) {
            EarlierRow(count = past.size, expanded = expanded, onClick = { expanded = !expanded })
            if (expanded) {
                past.forEach { block -> TimelineRow(block = block, position = TimelinePosition.Past) }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
            }
        }
        ahead.forEach { block ->
            val position =
                if (block.start <= now) TimelinePosition.Current else TimelinePosition.Future
            TimelineRow(block = block, position = position)
        }
        Text(
            text = "Schedule ends at ${hhmm(plan.blocks.last().end)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** The one row the finished blocks collapse into. */
@Composable
private fun EarlierRow(
    count: Int,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = if (count == 1) "1 block done" else "$count blocks done",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = if (expanded) "Hide" else "Show",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun TodayState.stage(): BlockKind? =
    when (this) {
        is TodayState.Running -> stage
        else -> null
    }

private fun TodayUiState.subtitle(): String {
    val plan =
        when (val day = today) {
            is TodayState.Running -> day.plan
            is TodayState.Skipped -> day.plan
            is TodayState.OffHours -> day.today
        }
    val name = plan.schedule?.name
    return listOfNotNull(dayLabel(plan.date), name).joinToString(" · ")
}

private fun TodayState.caption(): String =
    when (this) {
        is TodayState.Running -> {
            sessionCaption(nextBoundary, session.end)
        }

        is TodayState.Skipped -> {
            "Skipped until ${dayLabel(resumesOn)}"
        }

        is TodayState.OffHours -> {
            next?.let { "Next: ${it.scheduleName}, ${dayLabel(it.date)} ${hhmm(it.at)}" } ?: "No schedule runs this week"
        }
    }
