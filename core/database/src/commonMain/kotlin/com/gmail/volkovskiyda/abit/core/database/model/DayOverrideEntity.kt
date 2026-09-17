package com.gmail.volkovskiyda.abit.core.database.model

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.gmail.volkovskiyda.abit.core.model.DayOverride
import com.gmail.volkovskiyda.abit.core.model.decodeBoundaries
import com.gmail.volkovskiyda.abit.core.model.encodeBoundaries
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * One day's departure from the schedules. A row exists only once the user has skipped that day or
 * skipped a boundary on it, so the table stays a handful of rows however long the app is used.
 */
@Entity(tableName = "day_overrides")
data class DayOverrideEntity(
    @PrimaryKey val epochDay: Long,
    /**
     * The column is still `paused`: the rename happened in the domain, and renaming a column that
     * holds the same boolean would be a migration that changes no data and can only go wrong.
     */
    @ColumnInfo(name = "paused") val skipped: Boolean,
    /** Comma-separated minutes of day; empty string for none. A set of times has no useful index. */
    val skippedBoundaries: String,
    /** Last local modification. Last-write-wins reconciliation compares on this. */
    val updatedAtMillis: Long,
)

fun DayOverrideEntity.toModel(): DayOverride =
    DayOverride(
        date = LocalDate.fromEpochDays(epochDay),
        skipped = skipped,
        skippedBoundaries = skippedBoundaries.decodeBoundaries(),
        updatedAt = Instant.fromEpochMilliseconds(updatedAtMillis),
    )

fun DayOverride.toEntity(): DayOverrideEntity =
    DayOverrideEntity(
        epochDay = date.toEpochDays(),
        skipped = skipped,
        skippedBoundaries = skippedBoundaries.encodeBoundaries(),
        updatedAtMillis = updatedAt.toEpochMilliseconds(),
    )
