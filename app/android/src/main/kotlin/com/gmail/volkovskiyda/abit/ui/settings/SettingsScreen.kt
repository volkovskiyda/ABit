package com.gmail.volkovskiyda.abit.ui.settings

import android.Manifest
import android.os.Build
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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

    // Both permissions are granted in a system screen the user leaves the app for, so the only
    // reliable moment to re-read them is coming back.
    OnResume {
        viewModel.onPermissionsChanged(
            listOf(
                PermissionState(PermissionId.Notifications, permissions.canPostNotifications()),
                PermissionState(PermissionId.ExactAlarms, permissions.canScheduleExactAlarms()),
            ),
        )
    }

    val notificationLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            viewModel.onPermissionsChanged(
                state.permissions.map { if (it.id == PermissionId.Notifications) it.copy(granted = granted) else it },
            )
        }

    SettingsContent(
        state = state,
        onThemeMode = viewModel::setThemeMode,
        onChimeOnThisDevice = viewModel::setChimeOnThisDevice,
        onVibrate = viewModel::setVibrate,
        onShowCountdown = viewModel::setShowCountdownNotification,
        onSignIn = onOpenSignIn,
        onSignOut = viewModel::signOut,
        onRequestNotifications = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                context.startActivity(permissions.notificationSettingsIntent())
            }
        },
        onRequestExactAlarms = { context.startActivity(permissions.exactAlarmSettingsIntent()) },
        modifier = modifier,
    )
}

@Suppress("LongParameterList")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    state: SettingsUiState,
    onThemeMode: (ThemeMode) -> Unit,
    onChimeOnThisDevice: (Boolean) -> Unit,
    onVibrate: (Boolean) -> Unit,
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
                    .padding(horizontal = 16.dp),
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
                    title = "Chime on this device",
                    subtitle = "Turn off to keep this device quiet. Your other devices still chime.",
                    checked = state.preferences.chimeOnThisDevice,
                    onCheckedChange = onChimeOnThisDevice,
                )
                SwitchRow(
                    title = "Vibrate",
                    subtitle = null,
                    checked = state.preferences.vibrate,
                    onCheckedChange = onVibrate,
                )
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
