package com.gmail.volkovskiyda.abit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gmail.volkovskiyda.abit.core.domain.SyncState
import com.gmail.volkovskiyda.abit.feature.pomodoro.impl.PomodoroUiState
import com.gmail.volkovskiyda.abit.feature.pomodoro.impl.PomodoroViewModel
import com.gmail.volkovskiyda.abit.ui.theme.AbitTheme

@Composable
fun PomodoroScreen(viewModel: PomodoroViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PomodoroContent(state)
}

/**
 * Stateless on purpose: a preview, a screenshot test and a Compose UI test can all render it
 * without a dependency graph behind them.
 */
@Composable
fun PomodoroContent(
    state: PomodoroUiState,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = "ABit", style = MaterialTheme.typography.headlineMedium)
            Text(text = "Sessions: ${state.sessions.size}")
            Text(text = syncLabel(state.syncState))
        }
    }
}

private fun syncLabel(syncState: SyncState): String =
    when (syncState) {
        // Not an error: this is what a build without Firebase credentials reports, and the app is
        // fully usable — everything simply stays on the device.
        SyncState.Unavailable -> "Sync unavailable in this build"

        SyncState.SignedOut -> "Signed out"

        SyncState.Syncing -> "Syncing…"

        is SyncState.Idle -> "Synced"

        is SyncState.Failed -> "Sync failed: ${syncState.reason}"
    }

@Preview(showBackground = true)
@Composable
private fun PomodoroContentPreview() {
    AbitTheme {
        PomodoroContent(PomodoroUiState())
    }
}
