package com.gmail.volkovskiyda.abit.core.sync

import com.gmail.volkovskiyda.abit.core.model.PomodoroSession
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * How a session is stored in Firestore.
 *
 * Deliberately not `core:model`'s [PomodoroSession]: this shape is a wire format that other devices
 * and future app versions have to keep reading, so it changes on its own schedule. Times are epoch
 * milliseconds rather than Firestore `Timestamp`s, because the same numbers go into SQLite and one
 * representation across both stores is worth more than the server-side ordering a Timestamp buys.
 */
@Serializable
data class SessionDocument(
    val id: String = "",
    val startedAtMillis: Long = 0,
    val durationMinutes: Int = 0,
    val completed: Boolean = false,
    val updatedAtMillis: Long = 0,
    /** Which device last wrote this, for debugging a divergence. Never used to resolve one. */
    val deviceId: String = "",
)

fun SessionDocument.toModel(): PomodoroSession =
    PomodoroSession(
        id = id,
        startedAt = Instant.fromEpochMilliseconds(startedAtMillis),
        durationMinutes = durationMinutes,
        completed = completed,
        updatedAt = Instant.fromEpochMilliseconds(updatedAtMillis),
    )

fun PomodoroSession.toDocument(deviceId: String): SessionDocument =
    SessionDocument(
        id = id,
        startedAtMillis = startedAt.toEpochMilliseconds(),
        durationMinutes = durationMinutes,
        completed = completed,
        updatedAtMillis = updatedAt.toEpochMilliseconds(),
        deviceId = deviceId,
    )

/**
 * Every document this app writes lives under the user that owns it, which is what makes the security
 * rule a single line. `firestore.rules` denies everything outside this prefix.
 */
internal object FirestorePaths {
    const val USERS = "users"
    const val SESSIONS = "sessions"
}
