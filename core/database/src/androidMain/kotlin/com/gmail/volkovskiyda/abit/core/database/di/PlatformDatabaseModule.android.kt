package com.gmail.volkovskiyda.abit.core.database.di

import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.gmail.volkovskiyda.abit.core.database.ABIT_DATABASE_NAME
import com.gmail.volkovskiyda.abit.core.database.AbitDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformDatabaseModule: Module =
    module {
        single<RoomDatabase.Builder<AbitDatabase>> {
            // The absolute path rather than the bare name: the multiplatform builder takes a path, and
            // getDatabasePath is what puts it where `adb shell run-as` and the backup rules expect.
            Room.databaseBuilder<AbitDatabase>(
                context = androidContext(),
                name = androidContext().getDatabasePath(ABIT_DATABASE_NAME).absolutePath,
            )
        }
        // Bundled rather than the platform's own SQLite, so every Android version runs the same engine
        // the desktop build does — no version-dependent query planner surprises between them.
        single<SQLiteDriver> { BundledSQLiteDriver() }
    }
