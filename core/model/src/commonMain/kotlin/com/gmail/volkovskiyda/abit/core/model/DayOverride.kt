package com.gmail.volkovskiyda.abit.core.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * One day's departure from whatever the schedules say. Absent for almost every date — a row exists
 * only once the user has paused a day or skipped a boundary on it.
 */
@Serializable
data class DayOverride(
    val date: LocalDate,
    /** The whole day is silent. Written by "Pause today" and by "Pause tomorrow" in off hours. */
    val paused: Boolean = false,
    /** Boundaries whose chime is suppressed. The blocks themselves are unchanged. */
    val skippedBoundaries: Set<LocalTime> = emptySet(),
    /** Last local modification, and the field last-write-wins reconciliation compares on. */
    val updatedAt: Instant,
)
