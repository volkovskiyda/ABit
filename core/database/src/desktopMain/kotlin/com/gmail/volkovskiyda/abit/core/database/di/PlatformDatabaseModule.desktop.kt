package com.gmail.volkovskiyda.abit.core.database.di

import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.gmail.volkovskiyda.abit.core.common.AppDirs
import com.gmail.volkovskiyda.abit.core.database.ABIT_DATABASE_NAME
import com.gmail.volkovskiyda.abit.core.database.AbitDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformDatabaseModule: Module =
    module {
        single<RoomDatabase.Builder<AbitDatabase>> {
            Room.databaseBuilder<AbitDatabase>(name = AppDirs.path(ABIT_DATABASE_NAME))
        }
        single<SQLiteDriver> { BundledSQLiteDriver() }
    }
