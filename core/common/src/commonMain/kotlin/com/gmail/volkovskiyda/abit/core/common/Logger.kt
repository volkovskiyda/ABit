package com.gmail.volkovskiyda.abit.core.common

/**
 * Logging, narrow on purpose: four levels and no formatting. Crash reporting is a separate concern
 * and lives in `core:observability`, which forwards what it needs to Crashlytics.
 */
interface Logger {
    fun debug(tag: String, message: String)
    fun info(tag: String, message: String)
    fun warn(tag: String, message: String, throwable: Throwable? = null)
    fun error(tag: String, message: String, throwable: Throwable? = null)
}

/** Writes to whatever the platform calls a console: logcat, stdout, or the browser console. */
expect class PlatformLogger() : Logger
