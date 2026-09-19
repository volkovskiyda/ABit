package com.gmail.volkovskiyda.abit.ui.schedules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gmail.volkovskiyda.abit.core.designsystem.components.DialMark
import com.gmail.volkovskiyda.abit.core.designsystem.components.ScheduleCard
import com.gmail.volkovskiyda.abit.core.designsystem.components.SignInCard
import com.gmail.volkovskiyda.abit.core.designsystem.timeRange
import com.gmail.volkovskiyda.abit.core.domain.Conflict
import com.gmail.volkovskiyda.abit.core.model.Schedule
import com.gmail.volkovskiyda.abit.core.model.ScheduleId
import com.gmail.volkovskiyda.abit.feature.schedules.impl.SchedulesUiState
import com.gmail.volkovskiyda.abit.feature.schedules.impl.SchedulesViewModel
import com.gmail.volkovskiyda.abit.ui.isWideWindow
import kotlinx.coroutines.launch

private const val SHORT_DAY_LENGTH = 3

private val GUTTER = 16.dp

/** The extended FAB (56.dp) plus the scaffold's spacing under it, so the last card clears it. */
private val FAB_CLEARANCE = 88.dp

/** Big enough to read as the app's mark rather than as an icon that lost its row. */
private val EMPTY_MARK_SIZE = 72.dp

/** Between the empty state's call to action and the sign-in card below it. */
private val SIGN_IN_GAP = 32.dp

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
    onOpenSignIn: () -> Unit,
    modifier: Modifier = Modifier,
    editorPane: @Composable (id: String?, paneKey: String) -> Unit = { _, _ -> },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (!isWideWindow()) {
        SchedulesContent(
            state = state,
            onToggle = viewModel::toggle,
            onOpenEditor = onOpenEditor,
            onOpenConflict = onOpenConflict,
            onOpenSignIn = onOpenSignIn,
            modifier = modifier,
        )
        return
    }

    val navigator = rememberListDetailPaneScaffoldNavigator<String?>()
    val scope = rememberCoroutineScope()
    // The detail pane has no back stack to throw an editor away with, so a blank draft is told apart
    // from the last one by this counter. Without it a second "New schedule" would reopen the view
    // model that already saved the first, which is the same schedule under a different name.
    var blankDrafts by rememberSaveable { mutableIntStateOf(0) }
    NavigableListDetailPaneScaffold(
        navigator = navigator,
        modifier = modifier,
        listPane = {
            AnimatedPane {
                SchedulesContent(
                    state = state,
                    onToggle = viewModel::toggle,
                    onOpenEditor = { id ->
                        if (id == null) blankDrafts++
                        scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, id) }
                    },
                    onOpenConflict = onOpenConflict,
                    onOpenSignIn = onOpenSignIn,
                )
            }
        },
        detailPane = {
            AnimatedPane {
                val selected = navigator.currentDestination?.contentKey
                editorPane(selected, selected ?: "blank-$blankDrafts")
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
    onOpenSignIn: () -> Unit,
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
            //
            // Withheld while the list is empty: the empty state carries the same call to action in
            // the middle of the screen, and two buttons reading "New schedule" are one ambiguous
            // target for a screen reader and for `onNodeWithText`.
            if (!state.isEmpty) {
                ExtendedFloatingActionButton(onClick = { onOpenEditor(null) }) { Text("New schedule") }
            }
        },
    ) { padding ->
        if (state.isEmpty) {
            SchedulesEmpty(
                offerSignIn = state.offersSignIn,
                onNewSchedule = { onOpenEditor(null) },
                onSignIn = onOpenSignIn,
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }
        // The scaffold's padding goes into `contentPadding`, not a `Modifier.padding`: as a modifier
        // it shrinks the viewport and clips the list at the FAB, so the last card can never scroll
        // clear of it. As content padding the list fills the scaffold and scrolls past the button.
        // The scaffold leaves the FAB out of its own bottom inset, hence FAB_CLEARANCE on top of it.
        val layoutDirection = LocalLayoutDirection.current
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    start = padding.calculateStartPadding(layoutDirection) + GUTTER,
                    top = padding.calculateTopPadding() + GUTTER,
                    end = padding.calculateEndPadding(layoutDirection) + GUTTER,
                    bottom = padding.calculateBottomPadding() + GUTTER + FAB_CLEARANCE,
                ),
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

/**
 * A fresh install, and the one screen where the app has nothing of its own to show. The call to
 * action sits in the middle rather than in the corner: an empty list with a floating button is a
 * screen that looks broken until you find the button.
 *
 * The sign-in card is the same one Settings and the sheet offer, worded for the case that brings
 * someone here — schedules that already exist, on another device. It is **below** the primary
 * action and only for an account that has none: signing in is never what a new user has to do
 * first, which is the rule the whole auth flow is built on.
 */
@Composable
private fun SchedulesEmpty(
    offerSignIn: Boolean,
    onNewSchedule: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // One scrolling column for the whole screen, rather than a centred column with the card
        // pinned under it. The card is ~180.dp and the block above it ~260.dp, so on the 360x640 dp
        // phone in the Test Lab matrix — the shortest screen the app supports — pinning the card
        // left the centred column ~64.dp to scroll inside, and "New schedule" sat below the fold
        // with nothing on screen saying it was there. The FAB is withheld while the list is empty,
        // so that was a fresh install with no way at all to make a schedule.
        //
        // `heightIn(min = maxHeight)` is what keeps the centring: the column fills the screen and
        // centres its content when that content fits, and grows past the screen and scrolls when it
        // does not. Order is priority — the action is above the card, so the card is what goes
        // under the fold first. Signing in therefore re-centres the block rather than leaving the
        // headline where it was, which is the one thing the pinned card did better.
        Column(
            modifier =
                Modifier
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DialMark(size = EMPTY_MARK_SIZE, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(24.dp))
            Text(
                "No schedules yet",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "A schedule is the hours ABit chimes through — say 09:00 to 18:00 on weekdays, " +
                    "split into focus and breaks.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onNewSchedule) { Text("New schedule") }
            if (offerSignIn) {
                Spacer(Modifier.height(SIGN_IN_GAP))
                SignInCard(
                    onSignIn = onSignIn,
                    title = "Already use ABit elsewhere?",
                    body = "Sign in with Google to bring the schedules from your phone, watch, Mac and browser here.",
                )
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
