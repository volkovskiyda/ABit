package com.gmail.volkovskiyda.abit.core.sync

import com.gmail.volkovskiyda.abit.core.model.DayOverride
import com.gmail.volkovskiyda.abit.core.model.Schedule
import com.gmail.volkovskiyda.abit.core.model.ScheduleId
import com.gmail.volkovskiyda.abit.core.model.decodeBoundaries
import com.gmail.volkovskiyda.abit.core.model.encodeBoundaries
import com.gmail.volkovskiyda.abit.core.model.localTimeOfMinute
import com.gmail.volkovskiyda.abit.core.model.toDayOfWeekSet
import com.gmail.volkovskiyda.abit.core.model.toDaysMask
import com.gmail.volkovskiyda.abit.core.model.toMinuteOfDay
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * How a schedule is stored in Firestore.
 *
 * Deliberately not `core:model`'s [Schedule]: this shape is a wire format that other devices and
 * future app versions have to keep reading, so it changes on its own schedule. Times are epoch
 * milliseconds rather than Firestore `Timestamp`s, because the same numbers go into SQLite and one
 * representation across both stores is worth more than the server-side ordering a Timestamp buys.
 *
 * Every field has a default: a document written by a future version with an extra field still reads,
 * and one written by an older version without a field still reads too.
 */
@Serializable
data class ScheduleDocument(
    val id: String = "",
    val name: String = "",
    val enabled: Boolean = true,
    val daysMask: Int = 0,
    val startMinuteOfDay: Int = 0,
    val endMinuteOfDay: Int = 0,
    val focusMinutes: Int = 0,
    val breakMinutes: Int = 0,
    val updatedAtMillis: Long = 0,
    /** 0 means "not deleted". A nullable field would make every reader handle an absent key. */
    val deletedAtMillis: Long = 0,
    /** Which device last wrote this, for debugging a divergence. Never used to resolve one. */
    val deviceId: String = "",
)

@Serializable
data class DayOverrideDocument(
    val epochDay: Long = 0,
    val paused: Boolean = false,
    /** Comma-separated minutes of day, the same encoding the database uses. */
    val skippedBoundaries: String = "",
    val updatedAtMillis: Long = 0,
    val deviceId: String = "",
)

fun ScheduleDocument.toModel(): Schedule =
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
        deletedAt = deletedAtMillis.takeIf { it > 0 }?.let(Instant::fromEpochMilliseconds),
    )

fun Schedule.toDocument(deviceId: String): ScheduleDocument =
    ScheduleDocument(
        id = id.value,
        name = name,
        enabled = enabled,
        daysMask = days.toDaysMask(),
        startMinuteOfDay = start.toMinuteOfDay(),
        endMinuteOfDay = end.toMinuteOfDay(),
        focusMinutes = focusMinutes,
        breakMinutes = breakMinutes,
        updatedAtMillis = updatedAt.toEpochMilliseconds(),
        deletedAtMillis = deletedAt?.toEpochMilliseconds() ?: 0,
        deviceId = deviceId,
    )

fun DayOverrideDocument.toModel(): DayOverride =
    DayOverride(
        date = LocalDate.fromEpochDays(epochDay),
        paused = paused,
        skippedBoundaries = skippedBoundaries.decodeBoundaries(),
        updatedAt = Instant.fromEpochMilliseconds(updatedAtMillis),
    )

fun DayOverride.toDocument(deviceId: String): DayOverrideDocument =
    DayOverrideDocument(
        epochDay = date.toEpochDays(),
        paused = paused,
        skippedBoundaries = skippedBoundaries.encodeBoundaries(),
        updatedAtMillis = updatedAt.toEpochMilliseconds(),
        deviceId = deviceId,
    )

/** The document id of an override: `2026-09-15`, so a document is greppable in the console. */
fun DayOverride.documentId(): String = date.toString()

/**
 * Every document this app writes lives under the user that owns it, which is what makes the security
 * rule a single line. `firestore.rules` denies everything outside this prefix.
 */
internal object FirestorePaths {
    const val USERS = "users"
    const val SCHEDULES = "schedules"
    const val DAY_OVERRIDES = "dayOverrides"
}
