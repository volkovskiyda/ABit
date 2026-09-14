package com.gmail.volkovskiyda.abit.feature.pomodoro.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gmail.volkovskiyda.abit.core.domain.AuthRepository
import com.gmail.volkovskiyda.abit.core.domain.AuthUser
import com.gmail.volkovskiyda.abit.core.domain.PomodoroSessionRepository
import com.gmail.volkovskiyda.abit.core.domain.SyncState
import com.gmail.volkovskiyda.abit.core.domain.SyncStatusRepository
import com.gmail.volkovskiyda.abit.core.model.PomodoroSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * What the pomodoro screen renders. The timer itself is not built yet — this plan is
 * infrastructure — so the state carries the pieces every platform's UI already needs: the stored
 * sessions and whether they are syncing.
 */
data class PomodoroUiState(
    val sessions: List<PomodoroSession> = emptyList(),
    val syncState: SyncState = SyncState.Unavailable,
    /** `null` means signed out entirely; an anonymous user is still a user. */
    val user: AuthUser? = null,
    /** Set when a sign-in attempt failed, and cleared by the next one. */
    val authError: String? = null,
)

/**
 * Presentation logic lives here, in the feature module, on the multiplatform `ViewModel` — so the
 * phone, the watch, the tray app and the web app share it and only write their own Compose UI.
 */
class PomodoroViewModel(
    sessionRepository: PomodoroSessionRepository,
    syncStatusRepository: SyncStatusRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val authError = MutableStateFlow<String?>(null)

    val state: StateFlow<PomodoroUiState> =
        combine(
            sessionRepository.observeSessions(),
            syncStatusRepository.syncState,
            authRepository.currentUser,
            authError,
        ) { sessions, syncState, user, error ->
            PomodoroUiState(sessions = sessions, syncState = syncState, user = user, authError = error)
        }.stateIn(
            scope = viewModelScope,
            // Keeps the flow alive across a configuration change but not across a backgrounded app.
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = PomodoroUiState(),
        )

    /** Lets someone use the app with no account. Their sessions stay on the device until they link one. */
    fun signInAnonymously() = attempt { authRepository.signInAnonymously() }

    /**
     * Upgrades the current anonymous account rather than replacing it, so the history already on the
     * device survives and starts syncing. The id token comes from the platform's own sign-in UI.
     */
    fun signInWithGoogle(idToken: String) = attempt { authRepository.signInWithGoogle(idToken) }

    fun signOut() {
        viewModelScope.launch {
            authError.value = null
            authRepository.signOut()
        }
    }

    fun onAuthError(message: String) {
        authError.value = message
    }

    private fun attempt(block: suspend () -> Result<AuthUser>) {
        viewModelScope.launch {
            authError.value = null
            block().onFailure { authError.value = it.message ?: "Sign-in failed" }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
