package com.gmail.volkovskiyda.abit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gmail.volkovskiyda.abit.app.shared.initKoin
import com.gmail.volkovskiyda.abit.feature.pomodoro.impl.PomodoroUiState
import com.gmail.volkovskiyda.abit.feature.pomodoro.impl.PomodoroViewModel
import org.koin.compose.viewmodel.koinViewModel

fun main() {
    // Before `application`, not inside it: the composition may be recreated, and starting Koin
    // twice throws.
    initKoin()

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "ABit",
        ) {
            MaterialTheme {
                PomodoroScreen(viewModel = koinViewModel())
            }
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
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "ABit", style = MaterialTheme.typography.headlineMedium)
        Text(text = "Sessions: ${state.sessions.size}")
        Text(text = "Sync: ${state.syncState}")
    }
}
