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
    PomodoroWearContent(state)
}

@Composable
private fun PomodoroWearContent(state: PomodoroUiState) {
    ScreenScaffold {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = "ABit")
            Text(text = "Sessions: ${state.sessions.size}")
            Text(
                text = when (state.syncState) {
                    SyncState.Unavailable -> "No sync"
                    SyncState.SignedOut -> "Signed out"
                    SyncState.Syncing -> "Syncing…"
                    is SyncState.Idle -> "Synced"
                    is SyncState.Failed -> "Sync failed"
                },
            )
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
