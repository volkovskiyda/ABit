package com.gmail.volkovskiyda.abit.core.database.model

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.gmail.volkovskiyda.abit.core.model.DayOverride
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.time.Instant

private const val SECONDS_IN_MINUTE = 60

/**
 * One day's departure from the schedules. A row exists only once the user has paused that day or
 * skipped a boundary on it, so the table stays a handful of rows however long the app is used.
 */
@Entity(tableName = "day_overrides")
data class DayOverrideEntity(
    @PrimaryKey val epochDay: Long,
    val paused: Boolean,
    /** Comma-separated minutes of day; empty string for none. A set of times has no useful index. */
    val skippedBoundaries: String,
    /** Last local modification. Last-write-wins reconciliation compares on this. */
    val updatedAtMillis: Long,
)

fun DayOverrideEntity.toModel(): DayOverride =
    DayOverride(
        date = LocalDate.fromEpochDays(epochDay),
        paused = paused,
        skippedBoundaries =
            skippedBoundaries
                .split(",")
                .filter { it.isNotBlank() }
                .mapTo(mutableSetOf()) { LocalTime.fromSecondOfDay(it.trim().toInt() * SECONDS_IN_MINUTE) },
        updatedAt = Instant.fromEpochMilliseconds(updatedAtMillis),
    )

fun DayOverride.toEntity(): DayOverrideEntity =
    DayOverrideEntity(
        epochDay = date.toEpochDays(),
        paused = paused,
        skippedBoundaries =
            skippedBoundaries
                .map { it.toSecondOfDay() / SECONDS_IN_MINUTE }
                .sorted()
                .joinToString(","),
        updatedAtMillis = updatedAt.toEpochMilliseconds(),
    )
