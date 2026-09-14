package com.gmail.volkovskiyda.abit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gmail.volkovskiyda.abit.core.domain.SyncState
import com.gmail.volkovskiyda.abit.feature.pomodoro.impl.PomodoroUiState
import com.gmail.volkovskiyda.abit.feature.pomodoro.impl.PomodoroViewModel

/** The desktop has no system-wide light/dark signal Compose reads, so this follows the OS later. */
@Composable
fun AbitTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme(),
        content = content,
    )
}

@Composable
fun PomodoroPopup(viewModel: PomodoroViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PomodoroPopupContent(
        state = state,
        onSignInAnonymously = viewModel::signInAnonymously,
        onSignOut = viewModel::signOut,
    )
}

/** Stateless, so the desktop Compose UI test can render it without a graph. */
@Composable
fun PomodoroPopupContent(
    state: PomodoroUiState,
    modifier: Modifier = Modifier,
    onSignInAnonymously: () -> Unit = {},
    onSignOut: () -> Unit = {},
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
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

            Spacer(Modifier.height(24.dp))

            val user = state.user
            if (user == null) {
                Button(onClick = onSignInAnonymously) { Text("Use without an account") }
            } else {
                Text(if (user.isAnonymous) "Using ABit without an account" else "Signed in")
                Button(onClick = onSignOut) { Text("Sign out") }
            }
            // Google sign-in on this platform needs its own OAuth flow (a loopback redirect on the
            // desktop, a popup in the browser). That is a follow-up plan, not an oversight.
            Text("Google sign-in is coming to this platform", style = MaterialTheme.typography.bodySmall)

            state.authError?.let { error ->
                Spacer(Modifier.height(8.dp))
                Text(text = error, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
