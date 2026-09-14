package com.gmail.volkovskiyda.abit.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.gmail.volkovskiyda.abit.core.domain.SyncState
import com.gmail.volkovskiyda.abit.feature.pomodoro.impl.PomodoroUiState
import com.gmail.volkovskiyda.abit.feature.pomodoro.impl.PomodoroViewModel
import org.koin.androidx.compose.koinViewModel
import androidx.compose.ui.tooling.preview.Preview as ComposePreview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                AppScaffold {
                    PomodoroWearScreen(viewModel = koinViewModel())
                }
            }
        }
    }
}

@Composable
private fun PomodoroWearScreen(viewModel: PomodoroViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PomodoroWearContent(state, onSignInAnonymously = viewModel::signInAnonymously)
}

@Composable
private fun PomodoroWearContent(
    state: PomodoroUiState,
    onSignInAnonymously: () -> Unit = {},
) {
    ScreenScaffold {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = "ABit")
            Text(text = "Sessions: ${state.sessions.size}")
            Text(
                text =
                    when (state.syncState) {
                        SyncState.Unavailable -> "No sync"
                        SyncState.SignedOut -> "Signed out"
                        SyncState.Syncing -> "Syncing…"
                        is SyncState.Idle -> "Synced"
                        is SyncState.Failed -> "Sync failed"
                    },
            )
            // Anonymous only on the watch: signing in with Google means typing, and the watch syncs
            // through the cloud rather than through a paired phone, so it needs an account of its
            // own rather than the phone's.
            if (state.user == null) {
                Button(onClick = onSignInAnonymously) { Text("Start") }
            }
        }
    }
}

@ComposePreview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Composable
private fun PomodoroWearContentLargeRoundPreview() {
    MaterialTheme { AppScaffold { PomodoroWearContent(PomodoroUiState()) } }
}

@ComposePreview(device = WearDevices.SQUARE, showSystemUi = true)
@Composable
private fun PomodoroWearContentSquarePreview() {
    MaterialTheme { AppScaffold { PomodoroWearContent(PomodoroUiState()) } }
}
