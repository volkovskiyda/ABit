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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gmail.volkovskiyda.abit.core.datastore.ThemeMode
import com.gmail.volkovskiyda.abit.core.designsystem.components.PermissionRow
import com.gmail.volkovskiyda.abit.core.designsystem.components.SignInCard
import com.gmail.volkovskiyda.abit.core.domain.AuthUser
import com.gmail.volkovskiyda.abit.feature.settings.api.PermissionId
import com.gmail.volkovskiyda.abit.feature.settings.api.PermissionReader
import com.gmail.volkovskiyda.abit.feature.settings.impl.SettingsViewModel
import com.gmail.volkovskiyda.abit.web.auth.GoogleSignIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun WebSettingsScreen(modifier: Modifier = Modifier) {
    val viewModel: SettingsViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    // The same reader the view model was seeded from: the browser answers synchronously, so the row
    // is drawn with the real permission rather than with "not granted" until an effect corrects it.
    val permissionReader: PermissionReader = koinInject()
    // Remembered rather than injected: it holds nothing worth sharing, and its only input is a
    // committed constant plus whether Google's script finished loading.
    val googleSignIn = remember { GoogleSignIn() }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(Modifier.widthIn(max = CONTENT_MAX_WIDTH).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium)

            Section("ACCOUNT") {
                AccountCard(
                    user = state.user,
                    error = state.authError,
                    googleSignIn = googleSignIn,
                    onIdToken = viewModel::signInWithGoogle,
                    onError = viewModel::onAuthError,
                    onSignOut = viewModel::signOut,
                )
            }

            Section("PERMISSIONS") {
                PermissionRow(
                    title = "Browser notifications",
                    explanation = "Lets a chime show up even when this tab is behind another one.",
                    granted = state.permissions.firstOrNull { it.id == PermissionId.BrowserNotifications }?.granted == true,
                    actionLabel = "Allow",
                    onAction = {
                        // Notification.requestPermission() answers with a promise, and the row used
                        // to re-read the permission on the next line — before the browser had even
                        // shown the prompt. It said "not granted" however the user answered, until
                        // something else remounted the screen.
                        val answered = { viewModel.onPermissionsChanged(permissionReader.read()) }
                        val request = requestNotificationPermission()
                        if (request == null) answered() else request.then { answered() }
                    },
                )
            }

            Section("APPEARANCE") {
                SingleChoiceSegmentedButtonRow {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = state.preferences?.themeMode == mode,
                            // Nothing is selected until the stored mode has been read.
                            enabled = state.preferences != null,
                            onClick = { viewModel.setThemeMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                        ) { Text(mode.name) }
                    }
                }
            }

            // The browser is the one platform that is always current — a reload is the update. It
            // still says which build it is, because a bug report from a tab is as likely as one from
            // a watch and the number is what makes the two comparable.
            Section("ABOUT") {
                Text(
                    "ABit ${state.appVersion}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * The account section: sign in, who is signed in, or why signing in is not possible here.
 *
 * Anonymous reads as signed out — the account exists and Firebase has issued it a uid, but nothing
 * leaves this browser until it is linked to a Google one, and calling that "signed in" is a lie the
 * user discovers on their phone.
 */
@Composable
private fun AccountCard(
    user: AuthUser?,
    error: String?,
    googleSignIn: GoogleSignIn,
    onIdToken: (String) -> Unit,
    onError: (String) -> Unit,
    onSignOut: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val linked = user?.takeIf { !it.isAnonymous }

    // index.html loads Google's script `async defer`, so on a cold open this composes before
    // `google.accounts.id` exists. Reading it as a plain value meant the card said sync was
    // unavailable and nothing ever recomposed to correct it — a snapshot read is the only way a
    // JS global that appears later can reach Compose. Polling because the script tag is not ours
    // to attach a load handler to; it stops as soon as the answer is yes.
    val signInAvailable by produceState(googleSignIn.available, googleSignIn) {
        while (!value) {
            delay(GIS_POLL_INTERVAL)
            value = googleSignIn.available
        }
    }

    when {
        linked != null -> {
            SignInCard(
                onSignIn = onSignOut,
                title = "Syncing as ${linked.email ?: linked.displayName ?: "your Google account"}",
                body = "Your schedules are on your phone, your watch and your Mac too.",
                buttonLabel = "Sign out",
            )
        }

        // Honest rather than broken: with no web OAuth client on the project there is nothing to ask
        // Google for, and a button that cannot work is worse than a sentence saying why.
        !signInAvailable -> {
            SignInCard(
                onSignIn = {},
                title = "Sync is not available in this build",
                body =
                    "This copy of ABit has no Google sign-in configured, so everything stays in this " +
                        "browser — and after a reload it is still here.",
                buttonLabel = "Unavailable",
            )
        }

        else -> {
            SignInCard(
                onSignIn = {
                    scope.launch {
                        googleSignIn
                            .requestIdToken()
                            // The token goes to the ViewModel, which **links** it to the anonymous
                            // account rather than replacing it, so schedules made in this browser
                            // before signing in survive and start syncing.
                            .onSuccess(onIdToken)
                            .onFailure { onError(it.message ?: "Sign-in failed") }
                    }
                },
                body =
                    error
                        ?: "Sign in with Google to keep the same schedules on your phone, watch, Mac and browser.",
            )
        }
    }
}

@Composable
private fun Section(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

/**
 * The promise `Notification.requestPermission()` settles when the user answers the prompt.
 *
 * An external declaration rather than a lambda inside the `js(…)` body: passing a Kotlin function
 * across the boundary is something Kotlin/Wasm supports on a declaration's parameters, and writing
 * the same call inside a `js(…)` string would hand the compiler a name it cannot check. The same
 * reasoning the GIS wrappers carry.
 */
private external interface PermissionRequest : JsAny {
    fun then(onSettled: (JsString) -> Unit)
}

/** Null where the browser has no Notification API at all, which is an answer of its own. */
private fun requestNotificationPermission(): PermissionRequest? =
    js("(typeof Notification !== 'undefined') ? Notification.requestPermission() : null")

/** How often the account card re-asks whether Google's script has finished loading. */
private val GIS_POLL_INTERVAL = 200.milliseconds
