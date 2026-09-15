package com.gmail.volkovskiyda.abit.wear.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import com.gmail.volkovskiyda.abit.core.designsystem.timeRange
import com.gmail.volkovskiyda.abit.core.model.ScheduleId
import com.gmail.volkovskiyda.abit.feature.schedules.impl.SchedulesUiState
import com.gmail.volkovskiyda.abit.feature.schedules.impl.SchedulesViewModel
import com.gmail.volkovskiyda.abit.feature.settings.api.PermissionState

/**
 * Read and toggle only. **There is deliberately no editor on the watch**: choosing days and times on
 * a 45 mm screen is worse than doing it anywhere else, and a conflicting schedule shows a warning
 * here while the resolution happens on a bigger screen. Do not add one.
 */
@Composable
fun WearSchedulesScreen(
    viewModel: SchedulesViewModel,
    permissions: List<PermissionState>,
    onFixPermission: (PermissionState) -> Unit,
    modifier: Modifier = Modifier,
    signIn: WearSignInState = WearSignInState(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    WearSchedulesContent(
        state = state,
        missingPermissions = permissions.filterNot { it.granted },
        onToggle = { id, enabled -> viewModel.toggle(id, enabled) },
        onFixPermission = onFixPermission,
        signIn = signIn,
        modifier = modifier,
    )
}

/**
 * The watch's sign-in, offered as a card at the top of this list — **not a launch wall**, and not a
 * screen of its own. Since item 03 only a Google-linked account syncs, so a watch left anonymous is a
 * watch whose schedules never arrive; saying so here is the smallest honest place to say it.
 */
data class WearSignInState(
    val needsSignIn: Boolean = false,
    val available: Boolean = false,
    val error: String? = null,
    val onSignIn: () -> Unit = {},
)

@Composable
fun WearSchedulesContent(
    state: SchedulesUiState,
    missingPermissions: List<PermissionState>,
    onToggle: (ScheduleId, Boolean) -> Unit,
    onFixPermission: (PermissionState) -> Unit,
    modifier: Modifier = Modifier,
    signIn: WearSignInState = WearSignInState(),
) {
    val listState = rememberScalingLazyListState()
    ScreenScaffold(modifier = modifier, scrollState = listState, timeText = { TimeText() }) { padding ->
        ScalingLazyColumn(
            state = listState,
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // The watch has no settings screen in the design, so a missing permission surfaces here —
            // otherwise the one device most likely to be silent would never say why.
            if (signIn.needsSignIn) {
                items(listOf(signIn)) { state ->
                    Card(onClick = state.onSignIn, modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(
                                if (state.available) "Sign in with Google" else "Sign in on your phone",
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Text(
                                text =
                                    state.error
                                        ?: if (state.available) {
                                            "Your schedules sync once you sign in."
                                        } else {
                                            "This watch is too old for Google sign-in here."
                                        },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            items(missingPermissions) { permission ->
                Card(onClick = { onFixPermission(permission) }, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("Allow ${permission.id.name}", style = MaterialTheme.typography.labelMedium)
                        Text(
                            "This watch cannot chime without it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            items(state.schedules) { schedule ->
                val conflicting = state.conflictsFor(schedule.id).isNotEmpty()
                SwitchButton(
                    checked = schedule.enabled,
                    onCheckedChange = { onToggle(schedule.id, it) },
                    label = { Text(if (conflicting) "⚠ ${schedule.name}" else schedule.name) },
                    secondaryLabel = { Text(timeRange(schedule.start, schedule.end)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (state.schedules.isEmpty()) {
                items(listOf(Unit)) {
                    Text(
                        "No schedules yet. Add one on your phone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
