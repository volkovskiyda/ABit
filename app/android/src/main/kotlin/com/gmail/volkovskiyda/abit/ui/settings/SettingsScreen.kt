package com.gmail.volkovskiyda.abit.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gmail.volkovskiyda.abit.core.chime.ChimePermissions
import com.gmail.volkovskiyda.abit.core.datastore.ThemeMode
import com.gmail.volkovskiyda.abit.core.designsystem.components.PermissionRow
import com.gmail.volkovskiyda.abit.core.designsystem.components.SignInCard
import com.gmail.volkovskiyda.abit.feature.settings.api.PermissionId
import com.gmail.volkovskiyda.abit.feature.settings.api.PermissionState
import com.gmail.volkovskiyda.abit.feature.settings.impl.SettingsUiState
import com.gmail.volkovskiyda.abit.feature.settings.impl.SettingsViewModel
import com.gmail.volkovskiyda.abit.ui.OnResume
import org.koin.compose.koinInject

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onOpenSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val permissions: ChimePermissions = koinInject()
    val context = LocalContext.current
    val activity = LocalActivity.current

    /**
     * Set while the user has asked for the countdown and the permission is not there yet. The answer
     * can arrive from the system prompt or from a trip to the notification settings, so the switch
     * finishes moving wherever it comes back from — and is saved, because that trip can take the
     * Activity with it.
     */
    var wantsCountdown by rememberSaveable { mutableStateOf(false) }
    var explainNotifications by rememberSaveable { mutableStateOf(false) }

    // Both permissions are granted in a system screen the user leaves the app for, so the only
    // reliable moment to re-read them is coming back.
    OnResume {
        val canPost = permissions.canPostNotifications()
        viewModel.onPermissionsChanged(
            listOf(
                PermissionState(PermissionId.Notifications, canPost),
                PermissionState(PermissionId.ExactAlarms, permissions.canScheduleExactAlarms()),
            ),
        )
        if (wantsCountdown && canPost) {
            wantsCountdown = false
            viewModel.setShowCountdownNotification(true)
        }
    }

    val notificationLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            viewModel.onPermissionsChanged(
                state.permissions.map { if (it.id == PermissionId.Notifications) it.copy(granted = granted) else it },
            )
            when {
                granted -> {
                    if (wantsCountdown) {
                        wantsCountdown = false
                        viewModel.setShowCountdownNotification(true)
                    }
                }

                // A refusal the system will not prompt about again answers `false` without ever
                // showing a dialog. The app's notification settings are the only route left, and
                // `wantsCountdown` survives the trip so the switch still lands on the way back.
                activity != null &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(
                        activity,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) -> {
                    context.startActivity(permissions.notificationSettingsIntent())
                }

                else -> {
                    wantsCountdown = false
                }
            }
        }

    val requestNotifications = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            // No runtime prompt exists below API 33; the switch in system settings is the whole story.
            context.startActivity(permissions.notificationSettingsIntent())
        }
    }

    SettingsContent(
        state = state,
        onThemeMode = viewModel::setThemeMode,
        // The countdown is the one setting that cannot work without a permission Android can refuse,
        // so asking is part of turning it on. It stays off until the answer is yes.
        onShowCountdown = { wanted ->
            when {
                !wanted -> {
                    viewModel.setShowCountdownNotification(false)
                }

                permissions.canPostNotifications() -> {
                    viewModel.setShowCountdownNotification(true)
                }

                else -> {
                    wantsCountdown = true
                    explainNotifications = true
                }
            }
        },
        onSignIn = onOpenSignIn,
        onSignOut = viewModel::signOut,
        onRequestNotifications = requestNotifications,
        onRequestExactAlarms = { context.startActivity(permissions.exactAlarmSettingsIntent()) },
        modifier = modifier,
    )

    if (explainNotifications) {
        NotificationRationale(
            onContinue = {
                explainNotifications = false
                requestNotifications()
            },
            onDismiss = {
                explainNotifications = false
                wantsCountdown = false
            },
        )
    }
}

/**
 * Why the countdown needs a permission, said before the system asks rather than after.
 *
 * Android gives an app one useful prompt: once it is refused, it stops appearing and the only way
 * back is the settings screen. Spending it on a user who does not yet know what they are agreeing to
 * is how an app ends up permanently unable to do the thing it was built for.
 */
@Composable
private fun NotificationRationale(
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ABit needs to post notifications") },
        text = {
            Text(
                "The countdown to the end of a session lives in a notification, so Android has to " +
                    "let ABit post one. It is silent and it stays put — it counts itself down and " +
                    "is replaced at each boundary.",
            )
        },
        confirmButton = { TextButton(onClick = onContinue) { Text("Continue") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } },
    )
}

@Suppress("LongParameterList")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    state: SettingsUiState,
    onThemeMode: (ThemeMode) -> Unit,
    onShowCountdown: (Boolean) -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestExactAlarms: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        // Zero content insets: the NavigationSuiteScaffold outside this one already owns the bottom
        // edge, and a second Scaffold applying the same inset pushes its own content — the FAB most
        // visibly — under the navigation bar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { TopAppBar(title = { Text("Settings", style = MaterialTheme.typography.headlineMedium) }) },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    // The bottom inset is the navigation suite's, not this Scaffold's, so the last
                    // section needs the gap spelled out or it ends flush against the bar.
                    .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Section("ACCOUNT") {
                val user = state.user
                if (user == null || user.isAnonymous) {
                    SignInCard(onSignIn = onSignIn)
                } else {
                    Text(user.email ?: "Signed in", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = onSignOut) { Text("Sign out") }
                }
            }

            Section("THIS DEVICE") {
                SwitchRow(
                    title = "Show countdown in notification",
                    subtitle = null,
                    checked = state.preferences.showCountdownNotification,
                    onCheckedChange = onShowCountdown,
                )
            }

            Section("PERMISSIONS") {
                PermissionRow(
                    title = "Notifications",
                    explanation = "Without this, a boundary passes silently.",
                    granted = state.granted(PermissionId.Notifications),
                    actionLabel = "Allow",
                    onAction = onRequestNotifications,
                )
                PermissionRow(
                    title = "Alarms & reminders",
                    explanation = "Without this, a chime can arrive a few minutes late.",
                    granted = state.granted(PermissionId.ExactAlarms),
                    actionLabel = "Allow",
                    onAction = onRequestExactAlarms,
                )
            }

            Section("APPEARANCE") {
                SingleChoiceSegmentedButtonRow {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = state.preferences.themeMode == mode,
                            onClick = { onThemeMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                        ) { Text(mode.name) }
                    }
                }
            }
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

private fun SettingsUiState.granted(id: PermissionId): Boolean = permissions.firstOrNull { it.id == id }?.granted == true
