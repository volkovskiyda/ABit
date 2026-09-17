package com.gmail.volkovskiyda.abit.core.datastore

import androidx.datastore.core.DataStore
import com.gmail.volkovskiyda.abit.core.common.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn

private const val TAG = "UserPreferences"

/**
 * The app's view of its own preferences. A thin wrapper over [DataStore] so callers never see the
 * storage type — which matters here because one platform's DataStore is not a DataStore at all (see
 * the wasm binding).
 *
 * The file is read once for the process, eagerly, and kept: every later reader gets the cached
 * value. Without that, each new collector paid for its own read, and a screen built after one — the
 * settings screen is rebuilt on every visit — spent its first frames on [UserPreferences]' defaults
 * before the stored values arrived, so the switches and the theme selector visibly flipped.
 */
class UserPreferencesRepository(
    private val dataStore: DataStore<UserPreferences>,
    scope: CoroutineScope,
    logger: Logger,
) {
    /**
     * What has been read so far, or `null` while the first read is still in flight — which is not
     * the same thing as [UserPreferences]' defaults, and a screen that draws a control must be able
     * to tell the two apart.
     */
    val cached: StateFlow<UserPreferences?> =
        dataStore.data
            // An unreadable file is a bad preference file, not a broken app: it costs the user
            // their settings and nothing else. Without this the failure reaches every collector —
            // and this one is the process's only read, so nothing downstream would ever resolve.
            .catch { failure ->
                logger.error(TAG, "Preferences could not be read; falling back to defaults", failure)
                emit(UserPreferences())
            }.stateIn(scope, SharingStarted.Eagerly, null)

    /** The stored preferences, from the first real read onwards. Never the defaults as a placeholder. */
    val preferences: Flow<UserPreferences> = cached.filterNotNull()

    suspend fun update(transform: (UserPreferences) -> UserPreferences) {
        dataStore.updateData(transform)
    }
}
