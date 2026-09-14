package com.gmail.volkovskiyda.abit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gmail.volkovskiyda.abit.auth.GoogleSignIn
import com.gmail.volkovskiyda.abit.core.domain.SyncState
import com.gmail.volkovskiyda.abit.feature.pomodoro.impl.PomodoroUiState
import com.gmail.volkovskiyda.abit.feature.pomodoro.impl.PomodoroViewModel
import com.gmail.volkovskiyda.abit.ui.theme.AbitTheme
import kotlinx.coroutines.launch

@Composable
fun PomodoroScreen(viewModel: PomodoroViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val googleSignIn = remember(context) { GoogleSignIn(context) }

    PomodoroContent(
        state = state,
        canSignInWithGoogle = googleSignIn.serverClientId != null,
        onSignInAnonymously = viewModel::signInAnonymously,
        onSignInWithGoogle = {
            val clientId = googleSignIn.serverClientId
            if (clientId == null) {
                viewModel.onAuthError("Google sign-in is not configured for this build")
            } else {
                scope.launch {
                    googleSignIn
                        .requestIdToken(clientId)
                        .onSuccess(viewModel::signInWithGoogle)
                        .onFailure { viewModel.onAuthError(it.message ?: "Sign-in failed") }
                }
            }
        },
        onSignOut = viewModel::signOut,
    )
}

/**
 * Stateless on purpose: a preview, a screenshot test and a Compose UI test can all render it
 * without a dependency graph behind them.
 */
@Composable
fun PomodoroContent(
    state: PomodoroUiState,
    modifier: Modifier = Modifier,
    canSignInWithGoogle: Boolean = false,
    onSignInAnonymously: () -> Unit = {},
    onSignInWithGoogle: () -> Unit = {},
    onSignOut: () -> Unit = {},
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

            Spacer(Modifier.height(24.dp))

            val user = state.user
            when {
                user == null -> {
                    Button(onClick = onSignInAnonymously) { Text("Use without an account") }
                    if (canSignInWithGoogle) {
                        Button(onClick = onSignInWithGoogle) { Text("Sign in with Google") }
                    }
                }

                // Anonymous is not "signed out": it is a real account whose data can still be
                // linked to a Google one later, which is what this button does.
                user.isAnonymous -> {
                    Text("Using ABit without an account")
                    if (canSignInWithGoogle) {
                        Button(onClick = onSignInWithGoogle) { Text("Sign in with Google to sync") }
                    }
                    Button(onClick = onSignOut) { Text("Sign out") }
                }

                else -> {
                    Text("Signed in as ${user.email ?: user.displayName ?: "Google user"}")
                    Button(onClick = onSignOut) { Text("Sign out") }
                }
            }

            state.authError?.let { error ->
                Spacer(Modifier.height(8.dp))
                Text(text = error, color = MaterialTheme.colorScheme.error)
            }
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
