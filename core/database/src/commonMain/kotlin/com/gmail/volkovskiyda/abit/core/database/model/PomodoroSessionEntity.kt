package com.gmail.volkovskiyda.abit.core.database.model

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.gmail.volkovskiyda.abit.core.model.PomodoroSession
import kotlin.time.Instant

/**
 * The stored shape of a session. Separate from `core:model`'s [PomodoroSession] on purpose: the
 * database's columns are free to change for storage reasons without dragging the type every feature
 * and the sync layer speak through along with them.
 */
@Entity(tableName = "pomodoro_sessions")
data class PomodoroSessionEntity(
    @PrimaryKey val id: String,
    val startedAtMillis: Long,
    val durationMinutes: Int,
    val completed: Boolean,
    /** Last local modification. Last-write-wins reconciliation compares on this. */
    val updatedAtMillis: Long,
)

fun PomodoroSessionEntity.toModel(): PomodoroSession =
    PomodoroSession(
        id = id,
        startedAt = Instant.fromEpochMilliseconds(startedAtMillis),
        durationMinutes = durationMinutes,
        completed = completed,
        updatedAt = Instant.fromEpochMilliseconds(updatedAtMillis),
    )

fun PomodoroSession.toEntity(): PomodoroSessionEntity =
    PomodoroSessionEntity(
        id = id,
        startedAtMillis = startedAt.toEpochMilliseconds(),
        durationMinutes = durationMinutes,
        completed = completed,
        updatedAtMillis = updatedAt.toEpochMilliseconds(),
    )
