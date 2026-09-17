package com.gmail.volkovskiyda.abit.core.datastore

import kotlinx.serialization.Serializable

/** How the app should look, whatever the system is doing. */
@Serializable
enum class ThemeMode { System, Light, Dark }

/**
 * What a boundary sounds like on this device. [Platform] is the system's own notification sound,
 * which is what Android and Wear use; [SoftBell] is the tone the desktop and the browser synthesize
 * for themselves, having no notification sound to borrow.
 */
@Serializable
enum class ChimeSound { SoftBell, Platform, Silent }

/**
 * Everything the app remembers about this device, as opposed to about this user — the per-user data
 * that syncs lives in Firestore. The serializer's default-on-failure behaviour means adding a field
 * here is not a migration.
 *
 * The chime settings live here rather than in Firestore on purpose: every signed-in device is meant
 * to chime at 09:45, and "not this one" is a statement about the laptop in the meeting room, not
 * about the person. Syncing it would silence the phone in their pocket too.
 */
@Serializable
data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.System,
    val hasSeenOnboarding: Boolean = false,
    /** Epoch milliseconds of the last successful sync, or null if it has never run. */
    val lastSyncedAtMillis: Long? = null,
    /** Per device, never synced: every device chimes unless this one is told not to. */
    val chimeOnThisDevice: Boolean = true,
    val chimeSound: ChimeSound = ChimeSound.Platform,
    val vibrate: Boolean = true,
    /**
     * Off until asked for: it is the one chime setting that needs a permission Android can refuse,
     * and a default-on switch that silently does nothing is worse than one the user turned on.
     */
    val showCountdownNotification: Boolean = false,
)
