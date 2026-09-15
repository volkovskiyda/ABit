package com.gmail.volkovskiyda.abit.core.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.gmail.volkovskiyda.abit.core.database.model.ScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    /** By start time then name, which is the order the schedules list renders and nothing else wants. */
    @Query("SELECT * FROM schedules WHERE deletedAtMillis IS NULL ORDER BY startMinuteOfDay, name")
    fun observeAll(): Flow<List<ScheduleEntity>>

    /** Sync needs the tombstones: a row deleted here has to reach the other devices as a deletion. */
    @Query("SELECT * FROM schedules")
    suspend fun allIncludingDeleted(): List<ScheduleEntity>

    /**
     * Tombstones are excluded, the same as [observeAll]. Everything that reaches for one row by id
     * is answering "show me this schedule" — the editor opening a key off the back stack, a delete
     * looking for something to stamp — and a soft-deleted row is not one. Without the filter a
     * schedule deleted on another device mid-edit loaded as an ordinary draft, saved with its
     * deletedAt intact, and disappeared again the moment it was written. Sync reads the tombstones
     * through [allIncludingDeleted], which is the one caller that wants them.
     */
    @Query("SELECT * FROM schedules WHERE id = :id AND deletedAtMillis IS NULL")
    suspend fun findById(id: String): ScheduleEntity?

    /** Upsert rather than insert-or-replace: sync writes the same row repeatedly. */
    @Upsert
    suspend fun upsert(schedule: ScheduleEntity)

    @Upsert
    suspend fun upsertAll(schedules: List<ScheduleEntity>)

    /** Called with `now − 90 days`, so tombstones do not accumulate for the life of the install. */
    @Query("DELETE FROM schedules WHERE deletedAtMillis IS NOT NULL AND deletedAtMillis < :before")
    suspend fun purgeTombstones(before: Long)
}
