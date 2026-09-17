package com.gmail.volkovskiyda.abit.feature.settings.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gmail.volkovskiyda.abit.core.chime.ChimePreview
import com.gmail.volkovskiyda.abit.core.common.AppVersion
import com.gmail.volkovskiyda.abit.core.datastore.ChimeSound
import com.gmail.volkovskiyda.abit.core.datastore.ThemeMode
import com.gmail.volkovskiyda.abit.core.datastore.UserPreferences
import com.gmail.volkovskiyda.abit.core.datastore.UserPreferencesRepository
import com.gmail.volkovskiyda.abit.core.domain.AuthRepository
import com.gmail.volkovskiyda.abit.core.domain.AuthUser
import com.gmail.volkovskiyda.abit.core.domain.SyncState
import com.gmail.volkovskiyda.abit.core.domain.SyncStatusRepository
import com.gmail.volkovskiyda.abit.feature.settings.api.PermissionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val STOP_TIMEOUT_MILLIS = 5_000L

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val user: AuthUser? = null,
    val syncState: SyncState = SyncState.Unavailable,
    /**
     * Filled by the platform, because which permissions are real depends on it and because the
     * answers change while the app is backgrounded — a screen re-reads them on resume.
     */
    val permissions: List<PermissionState> = emptyList(),
    val authError: String? = null,
    /**
     * The running build, shown because no ABit app updates itself — every one of them is installed
     * by hand from a GitHub release, so this is the only way a user can tell whether they are behind.
     */
    val appVersion: String = "",
)

class SettingsViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val authRepository: AuthRepository,
    private val chimePreview: ChimePreview,
    syncStatusRepository: SyncStatusRepository,
    appVersion: AppVersion,
) : ViewModel() {
    private val version = appVersion.name
    private val permissions = MutableStateFlow<List<PermissionState>>(emptyList())
    private val authError = MutableStateFlow<String?>(null)

    val state: StateFlow<SettingsUiState> =
        combine(
            preferencesRepository.preferences,
            authRepository.currentUser,
            syncStatusRepository.syncState,
            permissions,
            authError,
        ) { preferences, user, syncState, permissionStates, error ->
            SettingsUiState(
                preferences = preferences,
                user = user,
                syncState = syncState,
                permissions = permissionStates,
                authError = error,
                appVersion = version,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = SettingsUiState(appVersion = version),
        )

    /** Called by the screen on every resume: a permission can be granted or revoked outside the app. */
    fun onPermissionsChanged(states: List<PermissionState>) {
        permissions.value = states
    }

    fun setThemeMode(mode: ThemeMode) = update { it.copy(themeMode = mode) }

    /**
     * Picking a sound plays it.
     *
     * A sound whose effect only arrives at the next boundary is one the user cannot evaluate while
     * they are still looking at the picker — and on the web that same tap is the user gesture the
     * browser needs before it will play anything at all, so it is also the only moment the audio
     * context can be started.
     */
    fun setChimeSound(sound: ChimeSound) {
        update { it.copy(chimeSound = sound) }
        viewModelScope.launch { chimePreview.chime(sound) }
    }

    /**
     * The screen is what gates this on the notification permission — asking for one needs an Activity,
     * which is not something `commonMain` has. By the time this is called the answer is yes.
     */
    fun setShowCountdownNotification(enabled: Boolean) {
        update { it.copy(showCountdownNotification = enabled) }
        if (enabled) viewModelScope.launch { chimePreview.countdown() }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            authError.value = null
            authRepository.signInWithGoogle(idToken).onFailure { authError.value = it.message ?: "Sign-in failed" }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authError.value = null
            authRepository.signOut()
        }
    }

    fun onAuthError(message: String) {
        authError.value = message
    }

    private fun update(transform: (UserPreferences) -> UserPreferences) {
        viewModelScope.launch { preferencesRepository.update(transform) }
    }
}
