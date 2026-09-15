package com.gmail.volkovskiyda.abit.core.database.di

import androidx.room3.RoomDatabase
import androidx.sqlite.SQLiteDriver
import com.gmail.volkovskiyda.abit.core.common.DispatcherProvider
import com.gmail.volkovskiyda.abit.core.database.AbitDatabase
import com.gmail.volkovskiyda.abit.core.database.dao.DayOverrideDao
import com.gmail.volkovskiyda.abit.core.database.dao.PomodoroSessionDao
import com.gmail.volkovskiyda.abit.core.database.dao.ScheduleDao
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * The database itself is assembled the same way everywhere; only the builder and the driver differ,
 * and those come from [platformDatabaseModule].
 */
val databaseModule: Module =
    module {
        single<AbitDatabase> {
            get<RoomDatabase.Builder<AbitDatabase>>()
                .setDriver(get<SQLiteDriver>())
                // Queries run off the main thread on every platform that has one. On wasm this is
                // Dispatchers.Default, because the browser has a single thread and the real offloading
                // happens in the Web Worker the driver talks to.
                .setQueryCoroutineContext(get<DispatcherProvider>().io)
                // No build has ever shipped user data in this database — version 1 held a placeholder
                // table that no screen ever wrote. A hand-written migration would be ceremony with no
                // beneficiary, so a version bump drops and recreates. Revisit the moment a real
                // user's schedules live here.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
        }
        single<PomodoroSessionDao> { get<AbitDatabase>().pomodoroSessionDao() }
        single<ScheduleDao> { get<AbitDatabase>().scheduleDao() }
        single<DayOverrideDao> { get<AbitDatabase>().dayOverrideDao() }
    }

/** The `RoomDatabase.Builder` and `SQLiteDriver` for this platform. */
expect val platformDatabaseModule: Module
