package com.gmail.volkovskiyda.abit.core.database.di

import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.web.WebWorkerSQLiteDriver
import com.gmail.volkovskiyda.abit.core.database.ABIT_DATABASE_NAME
import com.gmail.volkovskiyda.abit.core.database.AbitDatabase
import org.koin.core.module.Module
import org.koin.dsl.module
import org.w3c.dom.MODULE
import org.w3c.dom.Worker
import org.w3c.dom.WorkerOptions
import org.w3c.dom.WorkerType

/**
 * The script the SQLite WASM build runs in. Served from the web app's static resources rather than
 * bundled by Kotlin, because a `Worker` is constructed from a URL the page can fetch.
 *
 * That script stores the database in OPFS through the `opfs-sahpool` VFS, so a reload keeps the
 * user's schedules. It falls back to an in-memory database, with a console warning, where OPFS is
 * unavailable — a private window, blocked third-party storage, some embedded webviews.
 */
private const val SQLITE_WORKER_SCRIPT = "sqlite-worker.js"

actual val platformDatabaseModule: Module =
    module {
        single<RoomDatabase.Builder<AbitDatabase>> {
            // The same name the worker gives the pool's internal file, so the two agree on which
            // database is being opened.
            Room.databaseBuilder<AbitDatabase>(name = ABIT_DATABASE_NAME)
        }
        // Single instance because it owns a `Worker`: every connection is a message round trip to
        // that one thread, and starting a second worker would mean a second, unrelated database.
        single<SQLiteDriver> {
            WebWorkerSQLiteDriver(
                // A module worker, because the official sqlite-wasm build is ESM only and so the worker
                // has to `import` it rather than use `importScripts`.
                Worker(SQLITE_WORKER_SCRIPT, WorkerOptions(type = WorkerType.MODULE)),
            )
        }
    }
