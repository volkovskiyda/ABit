package com.gmail.volkovskiyda.abit.feature.pomodoro.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gmail.volkovskiyda.abit.core.domain.PomodoroSessionRepository
import com.gmail.volkovskiyda.abit.core.domain.SyncState
import com.gmail.volkovskiyda.abit.core.domain.SyncStatusRepository
import com.gmail.volkovskiyda.abit.core.model.PomodoroSession
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * What the pomodoro screen renders. The timer itself is not built yet — this plan is
 * infrastructure — so the state carries the pieces every platform's UI already needs: the stored
 * sessions and whether they are syncing.
 */
data class PomodoroUiState(
    val sessions: List<PomodoroSession> = emptyList(),
    val syncState: SyncState = SyncState.Unavailable,
)

/**
 * Presentation logic lives here, in the feature module, on the multiplatform `ViewModel` — so the
 * phone, the watch, the tray app and the web app share it and only write their own Compose UI.
 */
class PomodoroViewModel(
    sessionRepository: PomodoroSessionRepository,
    syncStatusRepository: SyncStatusRepository,
) : ViewModel() {

    val state: StateFlow<PomodoroUiState> =
        combine(
            sessionRepository.observeSessions(),
            syncStatusRepository.syncState,
        ) { sessions, syncState ->
            PomodoroUiState(sessions = sessions, syncState = syncState)
        }.stateIn(
            scope = viewModelScope,
            // Keeps the flow alive across a configuration change but not across a backgrounded app.
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = PomodoroUiState(),
        )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
