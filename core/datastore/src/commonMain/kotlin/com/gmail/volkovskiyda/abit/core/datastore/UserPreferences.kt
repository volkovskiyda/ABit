package com.gmail.volkovskiyda.abit.core.datastore

import kotlinx.serialization.Serializable

/** How the app should look, whatever the system is doing. */
@Serializable
enum class ThemeMode { System, Light, Dark }

/**
 * Everything the app remembers about this device, as opposed to about this user — the per-user data
 * that syncs lives in Firestore. Placeholder fields: the pomodoro feature decides what else belongs
 * here, and the serializer's default-on-failure behaviour means adding a field is not a migration.
 */
@Serializable
data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.System,
    val hasSeenOnboarding: Boolean = false,
    /** Epoch milliseconds of the last successful sync, or null if it has never run. */
    val lastSyncedAtMillis: Long? = null,
)
