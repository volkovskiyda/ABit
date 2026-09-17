package com.gmail.volkovskiyda.abit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
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
import com.gmail.volkovskiyda.abit.core.domain.AuthUser
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
 * This is the popover's first pane; "Schedules" swaps it for [SchedulesPaneContent] in the same
 * window rather than opening a second one — see [PopoverSurface].
 */
@Composable
@Suppress("LongParameterList")
fun TrayPopoverContent(
    state: TodayUiState,
    onSkipToday: (Boolean) -> Unit,
    onSkipTomorrow: () -> Unit,
    onOpenSchedules: () -> Unit,
    onQuit: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * False on a build with no `oauth.properties`, which turns the account row into a sentence
     * rather than a button. Defaulted so the UI test can render the popover without one.
     */
    signInAvailable: Boolean = false,
    onSignIn: () -> Unit = {},
    onSignOut: () -> Unit = {},
    /**
     * The running build, in the footer. The DMG is installed by hand from a GitHub release and
     * never updates itself, so this is the only place the Mac says which one it is. Defaulted for
     * the UI test, which has no Koin graph to ask.
     */
    appVersion: String = "",
) {
    val today = state.today
    PopoverSurface(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        SessionRing(
            arcs = if (today is TodayState.Running) ringArcs(today.session, today.sessionRemaining) else RingArcs.Empty,
            stage = (today as? TodayState.Running)?.stage ?: BlockKind.Focus,
            diameter = POPOVER_RING_DIAMETER,
            strokeWidth = POPOVER_RING_STROKE,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                ModeLabel((today as? TodayState.Running)?.stage)
                when (today) {
                    is TodayState.Running -> {
                        CountdownText(today.stageRemaining, style = MaterialTheme.typography.headlineMedium)
                    }

                    is TodayState.Skipped -> {
                        Text("Skipped", style = MaterialTheme.typography.headlineMedium)
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
                    FilledTonalButton(onClick = { onSkipToday(true) }) { Text("Skip today") }
                }

                is TodayState.Skipped -> {
                    FilledTonalButton(onClick = { onSkipToday(false) }) { Text("Resume today") }
                }

                is TodayState.OffHours -> {
                    FilledTonalButton(onClick = onSkipTomorrow) { Text("Skip tomorrow") }
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

        AccountRow(
            user = state.user,
            error = state.authError,
            signInAvailable = signInAvailable,
            onSignIn = onSignIn,
            onSignOut = onSignOut,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Beside the buttons rather than on a line of its own: the popover is 440 dp tall and
            // the version is not worth a row of it.
            Text(
                text = "ABit $appVersion",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onOpenSchedules) { Text("Schedules") }
                TextButton(onClick = onQuit) { Text("Quit") }
            }
        }
    }
}

/**
 * The chrome every pane of the popover shares.
 *
 * It draws its own rounded background and hairline border because the window is `undecorated` and
 * `transparent` — that pair is what makes this feel like a popover rather than a small window. Each
 * pane fills it, which is why the window grows instead of spawning a second one: there is one
 * surface under the menu-bar item, and panes take turns in it.
 */
@Composable
internal fun PopoverSurface(
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .padding(16.dp),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

/**
 * Who the schedules belong to, in one line at the foot of the popover.
 *
 * Anonymous counts as signed out here even though Firebase has issued it a uid: the account exists,
 * but nothing syncs off this Mac until it is linked to a Google one, and saying "signed in" for that
 * state would be a lie the user only discovers on their phone.
 */
@Composable
private fun AccountRow(
    user: AuthUser?,
    error: String?,
    signInAvailable: Boolean,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
) {
    val linked = user?.takeIf { !it.isAnonymous }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = error ?: linked?.let { it.email ?: it.displayName ?: "Signed in" } ?: "Not syncing",
            style = MaterialTheme.typography.bodySmall,
            color =
                if (error != null) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            modifier = Modifier.weight(1f),
        )
        when {
            linked != null -> {
                TextButton(onClick = onSignOut) { Text("Sign out") }
            }

            signInAvailable -> {
                TextButton(onClick = onSignIn) { Text("Sign in…") }
            }

            // No button at all rather than one that reports itself broken on every click. The
            // sentence beside it is the whole message: this build cannot sync, and that is a
            // property of the build, not something the user can fix from here.
            else -> {
                Text(
                    "Sign-in unavailable",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
        is TodayState.Running -> "ends ${hhmm(session.end)}"
        is TodayState.Skipped -> "Skipped until ${dayLabel(resumesOn)}"
        is TodayState.OffHours -> next?.let { "${it.scheduleName}, ${hhmm(it.at)}" } ?: "No schedule this week"
    }
