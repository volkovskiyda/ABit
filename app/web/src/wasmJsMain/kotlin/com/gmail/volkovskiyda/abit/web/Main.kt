package com.gmail.volkovskiyda.abit.web

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeViewport
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gmail.volkovskiyda.abit.app.shared.initKoin
import com.gmail.volkovskiyda.abit.core.domain.SyncState
import com.gmail.volkovskiyda.abit.feature.pomodoro.impl.PomodoroUiState
import com.gmail.volkovskiyda.abit.feature.pomodoro.impl.PomodoroViewModel
import kotlinx.browser.document
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    initKoin()

    ComposeViewport(document.body!!) {
        MaterialTheme {
            PomodoroScreen(viewModel = koinViewModel())
        }
    }
}

@Composable
private fun PomodoroScreen(viewModel: PomodoroViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PomodoroContent(state)
}

@Composable
private fun PomodoroContent(state: PomodoroUiState) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = "ABit", style = MaterialTheme.typography.headlineMedium)
            Text(text = "Sessions: ${state.sessions.size}")
            Text(
                text =
                    when (state.syncState) {
                        SyncState.Unavailable -> "Sync unavailable in this build"
                        SyncState.SignedOut -> "Signed out"
                        SyncState.Syncing -> "Syncing…"
                        is SyncState.Idle -> "Synced"
                        is SyncState.Failed -> "Sync failed"
                    },
            )
        }
    }
}
