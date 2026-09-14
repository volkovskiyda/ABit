package com.gmail.volkovskiyda.abit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gmail.volkovskiyda.abit.feature.pomodoro.impl.PomodoroUiState
import com.gmail.volkovskiyda.abit.feature.pomodoro.impl.PomodoroViewModel
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
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

/** Stateless, so a preview and a screenshot test can render it without a graph behind it. */
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
