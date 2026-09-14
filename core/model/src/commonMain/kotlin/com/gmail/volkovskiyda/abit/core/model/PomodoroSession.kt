package com.gmail.volkovskiyda.abit.core.model

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.time.Instant

/** The signed-in user, or the anonymous one. Firestore documents live under `users/{id}`. */
@JvmInline
@Serializable
value class UserId(val value: String)

/**
 * One completed or in-flight focus interval. Placeholder shape: the real pomodoro feature (its own
 * plan) decides what a session carries. It exists now so the database, sync and repository layers
 * have a type to be built against.
 */
@Serializable
data class PomodoroSession(
    val id: String,
    val startedAt: Instant,
    val durationMinutes: Int,
    val completed: Boolean,
    /** Last local modification, and the field last-write-wins reconciliation compares on. */
    val updatedAt: Instant,
)
