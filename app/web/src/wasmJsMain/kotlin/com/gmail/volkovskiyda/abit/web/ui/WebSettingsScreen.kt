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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gmail.volkovskiyda.abit.core.chime.WEB_CHIME_LIMITATION
import com.gmail.volkovskiyda.abit.core.datastore.ThemeMode
import com.gmail.volkovskiyda.abit.core.designsystem.components.PermissionRow
import com.gmail.volkovskiyda.abit.core.designsystem.components.SignInCard
import com.gmail.volkovskiyda.abit.feature.settings.api.PermissionId
import com.gmail.volkovskiyda.abit.feature.settings.api.PermissionState
import com.gmail.volkovskiyda.abit.feature.settings.impl.SettingsViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WebSettingsScreen(modifier: Modifier = Modifier) {
    val viewModel: SettingsViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

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
                // Honest rather than broken: web Google sign-in is a backlog item, and a button that
                // cannot work is worse than a sentence that says so.
                SignInCard(
                    onSignIn = {},
                    title = "Sync is not available in the browser yet",
                    body =
                        "Signing in with Google works on the phone, the watch and the Mac. " +
                            "In the browser everything stays on this device — and after a reload it is still here.",
                    buttonLabel = "Coming soon",
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
