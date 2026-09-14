package com.gmail.volkovskiyda.abit.core.datastore

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow

/**
 * The app's view of its own preferences. A thin wrapper over [DataStore] so callers never see the
 * storage type — which matters here because one platform's DataStore is not a DataStore at all (see
 * the wasm binding).
 */
class UserPreferencesRepository(
    private val dataStore: DataStore<UserPreferences>,
) {
    val preferences: Flow<UserPreferences> = dataStore.data

    suspend fun update(transform: (UserPreferences) -> UserPreferences) {
        dataStore.updateData(transform)
    }
}
