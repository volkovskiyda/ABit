package com.gmail.volkovskiyda.abit.ui.schedules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gmail.volkovskiyda.abit.core.designsystem.components.ScheduleCard
import com.gmail.volkovskiyda.abit.core.designsystem.timeRange
import com.gmail.volkovskiyda.abit.core.domain.Conflict
import com.gmail.volkovskiyda.abit.core.model.Schedule
import com.gmail.volkovskiyda.abit.core.model.ScheduleId
import com.gmail.volkovskiyda.abit.feature.schedules.impl.SchedulesUiState
import com.gmail.volkovskiyda.abit.feature.schedules.impl.SchedulesViewModel
import com.gmail.volkovskiyda.abit.ui.isWideWindow
import kotlinx.coroutines.launch

private const val SHORT_DAY_LENGTH = 3

/**
 * On a compact window this pushes the editor as its own destination, exactly as item 11 had it. On a
 * medium or expanded one the editor is the detail pane beside the list.
 *
 * `NavigableListDetailPaneScaffold` rather than the plain scaffold on purpose: it is what makes the
 * system back gesture collapse the detail pane before it pops the destination, which is the part of
 * a two-pane layout that is easy to get subtly wrong and impossible to notice on a phone.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun SchedulesScreen(
    viewModel: SchedulesViewModel,
    onOpenEditor: (String?) -> Unit,
    onOpenConflict: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    editorPane: @Composable (String?) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (!isWideWindow()) {
        SchedulesContent(
            state = state,
            onToggle = viewModel::toggle,
            onOpenEditor = onOpenEditor,
            onOpenConflict = onOpenConflict,
            modifier = modifier,
        )
        return
    }

    val navigator = rememberListDetailPaneScaffoldNavigator<String?>()
    val scope = rememberCoroutineScope()
    NavigableListDetailPaneScaffold(
        navigator = navigator,
        modifier = modifier,
        listPane = {
            AnimatedPane {
                SchedulesContent(
                    state = state,
                    onToggle = viewModel::toggle,
                    onOpenEditor = { id -> scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, id) } },
                    onOpenConflict = onOpenConflict,
                )
            }
        },
        detailPane = {
            AnimatedPane {
                val selected = navigator.currentDestination?.contentKey
                editorPane(selected)
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulesContent(
    state: SchedulesUiState,
    onToggle: (ScheduleId, Boolean) -> Unit,
    onOpenEditor: (String?) -> Unit,
    onOpenConflict: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        // Zero content insets: the NavigationSuiteScaffold outside this one already owns the bottom
        // edge, and a second Scaffold applying the same inset pushes its own content — the FAB most
        // visibly — under the navigation bar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { TopAppBar(title = { Text("Schedules", style = MaterialTheme.typography.headlineMedium) }) },
        floatingActionButton = {
            // The one elevated component in the whole design. The single-content overload rather
            // than the text/icon pair: with an empty icon slot the pair renders a button whose label
            // never reaches the semantics tree, which is invisible to a screen reader and to a test.
            ExtendedFloatingActionButton(onClick = { onOpenEditor(null) }) { Text("New schedule") }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding =
                androidx.compose.foundation.layout
                    .PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.schedules, key = { it.id.value }) { schedule ->
                val conflicts = state.conflictsFor(schedule.id)
                ScheduleCard(
                    schedule = schedule,
                    onClick = { onOpenEditor(schedule.id.value) },
                    onToggle = { onToggle(schedule.id, it) },
                    overlapLabel = conflicts.firstOrNull()?.label(state.schedules, schedule.id),
                    onOverlapClick =
                        conflicts.firstOrNull()?.let { conflict ->
                            { onOpenConflict(conflict.first.value, conflict.second.value) }
                        },
                    conflictingDays = conflicts.flatMap { it.days }.toSet(),
                )
            }
            if (state.conflicts.isNotEmpty()) {
                item {
                    Text(
                        text = "Overlapping schedules: only one can stay on. Tap a card to change its hours.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** "Overlaps Evening study · Mon, Wed, Fri 17:00 – 18:00", from the other schedule's point of view. */
internal fun Conflict.label(
    schedules: List<Schedule>,
    self: ScheduleId,
): String {
    val otherId = if (first == self) second else first
    val other = schedules.firstOrNull { it.id == otherId }?.name ?: "another schedule"
    val days =
        days.sortedBy { it.ordinal }.joinToString(", ") {
            it.name
                .take(SHORT_DAY_LENGTH)
                .lowercase()
                .replaceFirstChar(Char::uppercase)
        }
    return "Overlaps $other · $days ${timeRange(from, to)}"
}
