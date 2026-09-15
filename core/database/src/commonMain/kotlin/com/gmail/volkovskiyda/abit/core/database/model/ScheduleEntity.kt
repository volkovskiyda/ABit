package com.gmail.volkovskiyda.abit.core.database.model

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.gmail.volkovskiyda.abit.core.model.Schedule
import com.gmail.volkovskiyda.abit.core.model.ScheduleId
import com.gmail.volkovskiyda.abit.core.model.localTimeOfMinute
import com.gmail.volkovskiyda.abit.core.model.toDayOfWeekSet
import com.gmail.volkovskiyda.abit.core.model.toDaysMask
import com.gmail.volkovskiyda.abit.core.model.toMinuteOfDay
import kotlin.time.Instant

/**
 * The stored shape of a schedule. Separate from `core:model`'s [Schedule] on purpose: the database's
 * columns are free to change for storage reasons without dragging the type every feature and the
 * sync layer speak through along with them.
 */
@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey val id: String,
    val name: String,
    val enabled: Boolean,
    /** Mon = bit 0 … Sun = bit 6. One integer beats a join table for a seven-element set. */
    val daysMask: Int,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val focusMinutes: Int,
    val breakMinutes: Int,
    /** Last local modification. Last-write-wins reconciliation compares on this. */
    val updatedAtMillis: Long,
    /** Soft delete. Reads filter these out; sync carries them so other devices learn about them. */
    val deletedAtMillis: Long?,
)

fun ScheduleEntity.toModel(): Schedule =
    Schedule(
        id = ScheduleId(id),
        name = name,
        enabled = enabled,
        days = daysMask.toDayOfWeekSet(),
        start = localTimeOfMinute(startMinuteOfDay),
        end = localTimeOfMinute(endMinuteOfDay),
        focusMinutes = focusMinutes,
        breakMinutes = breakMinutes,
        updatedAt = Instant.fromEpochMilliseconds(updatedAtMillis),
        deletedAt = deletedAtMillis?.let(Instant::fromEpochMilliseconds),
    )

fun Schedule.toEntity(): ScheduleEntity =
    ScheduleEntity(
        id = id.value,
        name = name,
        enabled = enabled,
        daysMask = days.toDaysMask(),
        startMinuteOfDay = start.toMinuteOfDay(),
        endMinuteOfDay = end.toMinuteOfDay(),
        focusMinutes = focusMinutes,
        breakMinutes = breakMinutes,
        updatedAtMillis = updatedAt.toEpochMilliseconds(),
        deletedAtMillis = deletedAt?.toEpochMilliseconds(),
    )
