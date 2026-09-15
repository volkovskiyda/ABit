package com.gmail.volkovskiyda.abit.core.database

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import com.gmail.volkovskiyda.abit.core.database.dao.DayOverrideDao
import com.gmail.volkovskiyda.abit.core.database.dao.PomodoroSessionDao
import com.gmail.volkovskiyda.abit.core.database.dao.ScheduleDao
import com.gmail.volkovskiyda.abit.core.database.model.DayOverrideEntity
import com.gmail.volkovskiyda.abit.core.database.model.PomodoroSessionEntity
import com.gmail.volkovskiyda.abit.core.database.model.ScheduleEntity

@Database(
    entities = [PomodoroSessionEntity::class, ScheduleEntity::class, DayOverrideEntity::class],
    version = 2,
    exportSchema = true,
)
@ConstructedBy(AbitDatabaseConstructor::class)
abstract class AbitDatabase : RoomDatabase() {
    abstract fun pomodoroSessionDao(): PomodoroSessionDao

    abstract fun scheduleDao(): ScheduleDao

    abstract fun dayOverrideDao(): DayOverrideDao
}

/**
 * Room generates the `actual` for every target it processes, so the expect declaration only has to
 * name the contract. The suppressions are what the Kotlin compiler needs to accept an expect with
 * no hand-written actual in this source tree.
 */
@Suppress("KotlinNoActualForExpect", "EXPECT_ACTUAL_IR_INCOMPATIBILITY")
expect object AbitDatabaseConstructor : RoomDatabaseConstructor<AbitDatabase> {
    override fun initialize(): AbitDatabase
}

const val ABIT_DATABASE_NAME: String = "abit.db"
