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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gmail.volkovskiyda.abit.core.chime.WEB_CHIME_LIMITATION
import com.gmail.volkovskiyda.abit.core.datastore.ThemeMode
import com.gmail.volkovskiyda.abit.core.designsystem.components.PermissionRow
import com.gmail.volkovskiyda.abit.core.designsystem.components.SignInCard
import com.gmail.volkovskiyda.abit.core.domain.AuthUser
import com.gmail.volkovskiyda.abit.feature.settings.api.PermissionId
import com.gmail.volkovskiyda.abit.feature.settings.api.PermissionState
import com.gmail.volkovskiyda.abit.feature.settings.impl.SettingsViewModel
import com.gmail.volkovskiyda.abit.web.auth.GoogleSignIn
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WebSettingsScreen(modifier: Modifier = Modifier) {
    val viewModel: SettingsViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Remembered rather than injected: it holds nothing worth sharing, and its only input is a
    // committed constant plus whether Google's script finished loading.
    val googleSignIn = remember { GoogleSignIn() }

    LaunchedEffect(Unit) {
        viewModel.onPermissionsChanged(
            listOf(PermissionState(PermissionId.BrowserNotifications, notificationsGranted())),
        )
    }

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

            Section("THIS DEVICE") {
                SwitchRow(
                    title = "Chime in this tab",
                    subtitle = WEB_CHIME_LIMITATION,
                    checked = state.preferences.chimeOnThisDevice,
                    onCheckedChange = viewModel::setChimeOnThisDevice,
                )
            }

            Section("PERMISSIONS") {
                PermissionRow(
                    title = "Browser notifications",
                    explanation = "Lets a chime show up even when this tab is behind another one.",
                    granted = state.permissions.firstOrNull { it.id == PermissionId.BrowserNotifications }?.granted == true,
                    actionLabel = "Allow",
                    onAction = {
                        requestNotificationPermission()
                        viewModel.onPermissionsChanged(
                            listOf(PermissionState(PermissionId.BrowserNotifications, notificationsGranted())),
                        )
                    },
                )
            }

            Section("APPEARANCE") {
                SingleChoiceSegmentedButtonRow {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = state.preferences.themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                        ) { Text(mode.name) }
                    }
                }
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
        !googleSignIn.available -> {
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

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun notificationsGranted(): Boolean = js("typeof Notification !== 'undefined' && Notification.permission === 'granted'")

private fun requestNotificationPermission(): Unit = js("{ if (typeof Notification !== 'undefined') { Notification.requestPermission(); } }")
