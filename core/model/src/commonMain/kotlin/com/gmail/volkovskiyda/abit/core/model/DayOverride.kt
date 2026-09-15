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

/**
 * Comma-separated minutes of day, empty for none — the encoding both the `skippedBoundaries` column
 * and the Firestore field use. A set of times has no useful index in either store.
 */
fun Set<LocalTime>.encodeBoundaries(): String = map { it.toMinuteOfDay() }.sorted().joinToString(",")

fun String.decodeBoundaries(): Set<LocalTime> =
    split(",")
        .filter { it.isNotBlank() }
        .mapTo(mutableSetOf()) { localTimeOfMinute(it.trim().toInt()) }
