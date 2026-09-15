package com.gmail.volkovskiyda.abit.core.database.di

import androidx.room3.RoomDatabase
import androidx.sqlite.SQLiteDriver
import com.gmail.volkovskiyda.abit.core.common.DispatcherProvider
import com.gmail.volkovskiyda.abit.core.database.AbitDatabase
import com.gmail.volkovskiyda.abit.core.database.dao.DayOverrideDao
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
                // Version 3 drops the placeholder table, and this is the **last** bump for which a
                // destructive fallback is free: versions 1 and 2 held only a table no screen ever
                // wrote, while 3 is the first schema a real user's schedules live in. The next bump
                // is a hand-written migration, not another drop.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
        }
        single<ScheduleDao> { get<AbitDatabase>().scheduleDao() }
        single<DayOverrideDao> { get<AbitDatabase>().dayOverrideDao() }
    }

/** The `RoomDatabase.Builder` and `SQLiteDriver` for this platform. */
expect val platformDatabaseModule: Module
