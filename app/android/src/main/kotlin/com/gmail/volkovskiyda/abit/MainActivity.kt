package com.gmail.volkovskiyda.abit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

class MainActivity : ComponentActivity() {
    // The user's own choice outranks the system's, which is why the theme is read from preferences
    // rather than from `isSystemInDarkTheme()` inside AbitTheme.
    private val preferences: UserPreferencesRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // The one window where the stored theme is genuinely unknown. Holding the splash through it
        // is what the splash is for: the alternative is drawing the app light and repainting it dark
        // once the file has been read. It ends on the first read — the failure path emits defaults —
        // and after a warm start the value is already there, so this never waits at all.
        splash.setKeepOnScreenCondition { preferences.cached.value == null }

        setContent {
            // Remembered: a Flow operator called straight in composition would build a new flow on
            // every recomposition and reset the collection.
            val themeFlow = remember(preferences) { preferences.cached.map { it?.themeMode ?: ThemeMode.System } }
            val themeMode by
                themeFlow.collectAsStateWithLifecycle(
                    initialValue = preferences.cached.value?.themeMode ?: ThemeMode.System,
                )
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
        // Emptied *and* painted out on the editor and the two sheets, rather than hidden. The
        // scaffold measures and paints its navigation component whether or not it holds items, so
        // dropping the items alone left a bar-height band of `surfaceContainer` under the editor
        // with nothing in it; a transparent container makes that band the scaffold's own background,
        // which is the colour every screen already draws.
        //
        // `NavigationSuiteScaffoldState.hide()` takes the band *back*, which reads better and is the
        // one thing that cannot be done here: the height it returns resizes the single content
        // region every destination shares. A list sitting at the end of its scroll has its offset
        // clamped against the taller viewport, and clamping is not reversible — Schedules came back
        // from the editor scrolled up by the height of the bar, and Today and Settings would too.
        // Leaving the band measured costs the editor those dp and keeps every scroll position.
        navigationSuiteColors =
            if (onDestination) {
                NavigationSuiteDefaults.colors()
            } else {
                NavigationSuiteDefaults.colors(
                    shortNavigationBarContainerColor = Color.Transparent,
                    navigationBarContainerColor = Color.Transparent,
                )
            },
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
            onBack = { backStack.lastOrNull()?.let { key -> backStack.dismiss(key) } },
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
                            // Offered from the empty state, to someone whose schedules are on
                            // another device. The same sheet the sync badge and Settings open.
                            onOpenSignIn = { backStack.add(SignInNavKey) },
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
                            onDone = { backStack.dismiss(key) },
                        )
                    }
                    entry<ScheduleConflictNavKey> { key ->
                        ConflictScreen(key = key, onDismiss = { backStack.dismiss(key) }) { id ->
                            backStack.dismiss(key)
                            backStack.add(ScheduleEditorNavKey(id))
                        }
                    }
                    entry<SettingsNavKey> {
                        SettingsScreen(
                            viewModel = koinViewModel(),
                            onOpenSignIn = { backStack.add(SignInNavKey) },
                        )
                    }
                    entry<SignInNavKey> { key ->
                        GoogleSignInSheet(onDismiss = { backStack.dismiss(key) })
                    }
                },
        )
    }
}

/**
 * Leaves [key], rather than whatever happens to be on top.
 *
 * Every entry that is not a destination has more than one way out — the editor's Cancel button and
 * the saved flag its view model raises, a sheet's scrim, its buttons and the effect that closes it
 * once the thing it is asking about is gone — and two of them can fire for the same entry. An entry
 * stays composed while it animates away, so an effect that reacts on the frame after the tap that
 * already popped it used to pop a second time and take the destination underneath with it. Popping
 * *this* key makes the second dismissal a no-op.
 *
 * Index 0 is left alone for the same reason: `NavDisplay` requires a back stack with something in
 * it, and one that empties does not misdraw, it throws — "NavDisplay backstack cannot be empty" —
 * on the next frame. Back arrives here too, so the root destination stays put and the gesture falls
 * through to the activity, which is what closes the app.
 */
private fun MutableList<NavKey>.dismiss(key: NavKey) {
    val index = lastIndexOf(key)
    if (index > 0) removeAt(index)
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
    var linking by rememberSaveable { mutableStateOf(false) }

    // Closed by the account changing, not by handing the token over. signInWithGoogle only *starts*
    // the link, in a viewModelScope belonging to this back-stack entry — so dismissing on the next
    // line tore the entry down and cancelled the link mid-flight, every time. A cancelled link is
    // what FirebaseAuthRepository used to read as a uid collision, which deleted the anonymous
    // account. Waiting for the user to actually change also puts any error in front of the person
    // who caused it, on the sheet that is still open.
    val linked = linking && state.user?.isAnonymous == false
    LaunchedEffect(linked) {
        if (linked) onDismiss()
    }

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
                            linking = true
                            viewModel.signInWithGoogle(it)
                        }.onFailure { viewModel.onAuthError(it.message ?: "Sign-in failed") }
                }
            }
        },
        onDismiss = onDismiss,
        error = state.authError,
    )
}
