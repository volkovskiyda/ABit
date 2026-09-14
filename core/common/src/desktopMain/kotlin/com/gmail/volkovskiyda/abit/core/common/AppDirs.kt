package com.gmail.volkovskiyda.abit.core.common

import java.io.File

/**
 * Where the desktop app keeps its own files — the database, DataStore and the Firebase token cache.
 *
 * macOS is the platform ABit ships a desktop build for, so it gets the location Apple expects;
 * the other two branches exist so a developer running `:app:desktop:run` on Linux or Windows does
 * not scatter files into their home directory.
 */
object AppDirs {
    private const val APP_NAME = "ABit"

    val dataDirectory: File by lazy {
        val os = System.getProperty("os.name").orEmpty().lowercase()
        val home = File(System.getProperty("user.home"))
        val base =
            when {
                os.contains("mac") -> File(home, "Library/Application Support")
                os.contains("win") -> System.getenv("APPDATA")?.let(::File) ?: File(home, "AppData/Roaming")
                else -> System.getenv("XDG_DATA_HOME")?.let(::File) ?: File(home, ".local/share")
            }
        File(base, APP_NAME).apply { mkdirs() }
    }

    /** Absolute path of [name] inside the data directory, which is created if it does not exist. */
    fun path(name: String): String = File(dataDirectory, name).absolutePath
}
