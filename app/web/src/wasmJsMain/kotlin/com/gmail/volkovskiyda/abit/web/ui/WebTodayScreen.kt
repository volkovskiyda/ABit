package com.gmail.volkovskiyda.abit.web.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gmail.volkovskiyda.abit.core.chime.WEB_CHIME_LIMITATION
import com.gmail.volkovskiyda.abit.core.designsystem.RingArcs
import com.gmail.volkovskiyda.abit.core.designsystem.components.AbitChip
import com.gmail.volkovskiyda.abit.core.designsystem.components.CountdownText
import com.gmail.volkovskiyda.abit.core.designsystem.components.ModeLabel
import com.gmail.volkovskiyda.abit.core.designsystem.components.SessionRing
import com.gmail.volkovskiyda.abit.core.designsystem.components.TimelinePosition
import com.gmail.volkovskiyda.abit.core.designsystem.components.TimelineRow
import com.gmail.volkovskiyda.abit.core.designsystem.dayLabel
import com.gmail.volkovskiyda.abit.core.designsystem.hhmm
import com.gmail.volkovskiyda.abit.core.designsystem.ringArcs
import com.gmail.volkovskiyda.abit.core.designsystem.sessionCaption
import com.gmail.volkovskiyda.abit.core.designsystem.skippedCaption
import com.gmail.volkovskiyda.abit.core.domain.BlockKind
import com.gmail.volkovskiyda.abit.core.domain.TodayState
import com.gmail.volkovskiyda.abit.feature.today.impl.TodayViewModel
import org.koin.compose.viewmodel.koinViewModel

/** The design's content ceiling, so a desktop-width browser does not stretch a line of text. */
internal val CONTENT_MAX_WIDTH = 1200.dp

@Composable
fun WebTodayScreen(modifier: Modifier = Modifier) {
    val viewModel: TodayViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val today = state.today

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.widthIn(max = CONTENT_MAX_WIDTH).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Today", style = MaterialTheme.typography.headlineMedium)

            // Said out loud rather than hidden: a browser throttles a background tab's timers and
            // stops a discarded one, so the web is the one surface that cannot promise a chime.
            AbitChip(text = WEB_CHIME_LIMITATION)

            Row(horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.weight(2f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    SessionRing(
                        arcs = if (today is TodayState.Running) ringArcs(today.session, today.sessionRemaining) else RingArcs.Empty,
                        stage = (today as? TodayState.Running)?.stage ?: BlockKind.Focus,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            ModeLabel((today as? TodayState.Running)?.stage)
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
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        when (today) {
                            is TodayState.Running -> {
                                FilledTonalButton(onClick = { viewModel.skipToday(true) }) { Text("Skip today") }
                            }

                            is TodayState.Skipped -> {
                                FilledTonalButton(onClick = { viewModel.skipToday(false) }) { Text("Resume today") }
                            }

                            is TodayState.OffHours -> {
                                FilledTonalButton(onClick = { viewModel.skipTomorrow() }) { Text("Skip tomorrow") }
                            }
                        }
                    }
                }

                Column(Modifier.weight(3f)) {
                    val plan = today.plan()
                    if (plan.sessions.isNotEmpty()) {
                        Text("Rest of today", style = MaterialTheme.typography.titleMedium)
                        val now = (today as? TodayState.Running)?.nextBoundary
                        plan.blocks.forEach { block ->
                            TimelineRow(
                                block = block,
                                position =
                                    when {
                                        now == null -> TimelinePosition.Future
                                        block.end < now -> TimelinePosition.Past
                                        block.end == now -> TimelinePosition.Current
                                        else -> TimelinePosition.Future
                                    },
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun TodayState.plan() =
    when (this) {
        is TodayState.Running -> plan
        is TodayState.Skipped -> plan
        is TodayState.OffHours -> today
    }

private fun TodayState.caption(): String =
    when (this) {
        is TodayState.Running -> {
            sessionCaption(nextBoundary, session.end)
        }

        is TodayState.Skipped -> {
            skippedCaption(resumesOn)
        }

        is TodayState.OffHours -> {
            next?.let { "Next: ${it.scheduleName}, ${dayLabel(it.date)} ${hhmm(it.at)}" } ?: "No schedule runs this week"
        }
    }
