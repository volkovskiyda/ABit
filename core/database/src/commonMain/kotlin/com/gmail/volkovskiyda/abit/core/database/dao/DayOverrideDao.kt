package com.gmail.volkovskiyda.abit.core.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.gmail.volkovskiyda.abit.core.database.model.DayOverrideEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DayOverrideDao {
    /** Today onward: yesterday's override cannot change what any surface renders. */
    @Query("SELECT * FROM day_overrides WHERE epochDay >= :from ORDER BY epochDay")
    fun observeFrom(from: Long): Flow<List<DayOverrideEntity>>

    @Query("SELECT * FROM day_overrides")
    suspend fun all(): List<DayOverrideEntity>

    @Query("SELECT * FROM day_overrides WHERE epochDay = :epochDay")
    suspend fun findByEpochDay(epochDay: Long): DayOverrideEntity?

    @Upsert
    suspend fun upsert(override: DayOverrideEntity)

    @Upsert
    suspend fun upsertAll(overrides: List<DayOverrideEntity>)

    @Query("DELETE FROM day_overrides WHERE epochDay < :before")
    suspend fun purgeBefore(before: Long)
}
