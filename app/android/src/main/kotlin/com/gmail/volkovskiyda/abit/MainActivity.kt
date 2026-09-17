package com.gmail.volkovskiyda.abit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.gmail.volkovskiyda.abit.auth.GoogleSignIn
import com.gmail.volkovskiyda.abit.core.datastore.ThemeMode
import com.gmail.volkovskiyda.abit.core.datastore.UserPreferencesRepository
import com.gmail.volkovskiyda.abit.core.designsystem.components.DialMark
import com.gmail.volkovskiyda.abit.feature.schedules.api.ScheduleConflictNavKey
import com.gmail.volkovskiyda.abit.feature.schedules.api.ScheduleEditorNavKey
import com.gmail.volkovskiyda.abit.feature.schedules.api.SchedulesNavKey
import com.gmail.volkovskiyda.abit.feature.schedules.impl.SchedulesViewModel
import com.gmail.volkovskiyda.abit.feature.settings.api.SettingsNavKey
import com.gmail.volkovskiyda.abit.feature.settings.api.SignInNavKey
import com.gmail.volkovskiyda.abit.feature.today.api.TodayNavKey
import com.gmail.volkovskiyda.abit.feature.today.impl.TodayViewModel
import com.gmail.volkovskiyda.abit.ui.auth.SignInSheet
import com.gmail.volkovskiyda.abit.ui.schedules.ConflictSheet
import com.gmail.volkovskiyda.abit.ui.schedules.ScheduleEditorScreen
import com.gmail.volkovskiyda.abit.ui.schedules.SchedulesScreen
import com.gmail.volkovskiyda.abit.ui.settings.SettingsScreen
import com.gmail.volkovskiyda.abit.ui.theme.AbitTheme
import com.gmail.volkovskiyda.abit.ui.today.TodayScreen
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            // The user's own choice outranks the system's, which is why the theme is read from
            // preferences here rather than from `isSystemInDarkTheme()` inside AbitTheme.
            val preferences: UserPreferencesRepository = koinInject()
            // Remembered: a Flow operator called straight in composition would build a new flow on
            // every recomposition and reset the collection.
            val themeFlow = remember(preferences) { preferences.preferences.map { it.themeMode } }
            val themeMode by themeFlow.collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            AbitTheme(themeMode = themeMode) {
                AbitNavDisplay()
            }
        }
    }
}

/** The three destinations in the bottom bar, in the order the design draws them. */
private enum class Destination(
    val key: NavKey,
    val label: String,
) {
    Today(TodayNavKey, "Today"),
    Schedules(SchedulesNavKey, "Schedules"),
    Settings(SettingsNavKey, "Settings"),
}

/**
 * The `entryProvider { entry<K> { } }` DSL is deliberate: it is the shape the Kotzilla compiler
 * plugin rewrites to record screen views, so a screen is registered by adding a key here rather than
 * by hand-wrapping each composable.
 */
@Composable
internal fun AbitNavDisplay() {
    val backStack = rememberNavBackStack(TodayNavKey)
    val current = backStack.lastOrNull()
    val onDestination = Destination.entries.any { it.key == current }

    // One declaration for three form factors: a bottom bar on a compact width, a rail on medium and
    // expanded. The alternative was a second layout file for tablets that could drift from this one.
    NavigationSuiteScaffold(
        navigationSuiteItems = {
            if (onDestination) {
                Destination.entries.forEach { destination ->
                    item(
                        selected = destination.key == current,
                        onClick = {
                            // One entry per destination: tapping the bar switches rather than stacks.
                            backStack.removeAll { it in Destination.entries.map(Destination::key) }
                            backStack.add(destination.key)
                        },
                        icon = { DestinationIcon(destination) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            // NavDisplay's own default is the saveable-state holder and nothing else, so without
            // this every `koinViewModel()` below resolves against the *activity's* store: one
            // ScheduleEditorViewModel, shared by every visit to the editor. Tapping "New schedule"
            // after saving one returned that same finished view model, whose `saved` flag popped the
            // destination again before it drew. The store decorator scopes one per destination, and
            // it needs the saveable-state holder underneath it to hand out SavedStateHandles.
            entryDecorators =
                listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
            entryProvider =
                entryProvider<NavKey> {
                    entry<TodayNavKey> {
                        TodayScreen(
                            viewModel = koinViewModel<TodayViewModel>(),
                            onOpenSignIn = { backStack.add(SignInNavKey) },
                            onOpenConflict = { a, b -> backStack.add(ScheduleConflictNavKey(a, b)) },
                        )
                    }
                    entry<SchedulesNavKey> {
                        SchedulesScreen(
                            viewModel = koinViewModel<SchedulesViewModel>(),
                            onOpenEditor = { id -> backStack.add(ScheduleEditorNavKey(id)) },
                            onOpenConflict = { a, b -> backStack.add(ScheduleConflictNavKey(a, b)) },
                            // Only reached on a wide window, where the editor is the detail pane
                            // rather than a pushed destination.
                            editorPane = { id, paneKey ->
                                ScheduleEditorScreen(
                                    // Keyed by the pane so selecting another card — or asking for a
                                    // second blank draft — builds a new editor rather than reusing
                                    // the previous one's.
                                    viewModel = koinViewModel(key = "editor-$paneKey") { parametersOf(id) },
                                    onDone = {},
                                )
                            },
                        )
                    }
                    entry<ScheduleEditorNavKey> { key ->
                        ScheduleEditorScreen(
                            viewModel = koinViewModel { parametersOf(key.id) },
                            onDone = { backStack.removeLastOrNull() },
                        )
                    }
                    entry<ScheduleConflictNavKey> { key ->
                        ConflictScreen(key = key, onDismiss = { backStack.removeLastOrNull() }) { id ->
                            backStack.removeLastOrNull()
                            backStack.add(ScheduleEditorNavKey(id))
                        }
                    }
                    entry<SettingsNavKey> {
                        SettingsScreen(
                            viewModel = koinViewModel(),
                            onOpenSignIn = { backStack.add(SignInNavKey) },
                        )
                    }
                    entry<SignInNavKey> {
                        GoogleSignInSheet(onDismiss = { backStack.removeLastOrNull() })
                    }
                },
        )
    }
}

/** The Today destination wears the app's own dial, not a generic clock. */
@Composable
private fun DestinationIcon(destination: Destination) {
    when (destination) {
        Destination.Today -> DialMark(size = 24.dp)
        Destination.Schedules -> Text("▤", style = MaterialTheme.typography.titleMedium)
        Destination.Settings -> Text("⚙", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ConflictScreen(
    key: ScheduleConflictNavKey,
    onDismiss: () -> Unit,
    onEditHours: (String) -> Unit,
) {
    val viewModel = koinViewModel<SchedulesViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val conflict = state.conflicts.firstOrNull { it.first.value == key.first && it.second.value == key.second }

    // Someone resolved it on another device while this sheet was opening — but only once the
    // repository has actually answered. This ViewModel is scoped to this back-stack entry, so it is
    // always freshly created here and its first frame carries stateIn's empty initial value; the
    // sheet used to read that as "already resolved" and pop itself before it ever drew, from both
    // entry points. Popping from a LaunchedEffect rather than the composition body, too: mutating
    // the back stack while composing it is its own hazard.
    LaunchedEffect(state.loaded, conflict) {
        if (state.loaded && conflict == null) onDismiss()
    }
    if (conflict == null) return

    ConflictSheet(
        conflict = conflict,
        schedules = state.schedules,
        onKeep = { keep, disable ->
            viewModel.resolveConflict(keep, disable)
            onDismiss()
        },
        onEditHours = onEditHours,
        onDismiss = onDismiss,
    )
}

/**
 * The Google sheet. It signs in through Credential Manager and hands the id token to the ViewModel,
 * which **links** it to the anonymous account rather than replacing it — so the schedules written
 * before signing in survive and start syncing.
 *
 * A build with no web OAuth client has no `serverClientId`, and the sheet says so instead of offering
 * a button that cannot work. A keyless clone has to keep running.
 */
@Composable
private fun GoogleSignInSheet(onDismiss: () -> Unit) {
    val viewModel: TodayViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val googleSignIn = remember(context) { GoogleSignIn(context) }

    SignInSheet(
        onSignIn = {
            val clientId = googleSignIn.serverClientId
            if (clientId == null) {
                viewModel.onAuthError("Google sign-in is not configured for this build")
            } else {
                scope.launch {
                    googleSignIn
                        .requestIdToken(clientId)
                        .onSuccess {
                            viewModel.signInWithGoogle(it)
                            onDismiss()
                        }.onFailure { viewModel.onAuthError(it.message ?: "Sign-in failed") }
                }
            }
        },
        onDismiss = onDismiss,
        error = state.authError,
    )
}
