package com.gmail.volkovskiyda.abit.core.common

/**
 * Logging, narrow on purpose: four levels and no formatting. Crash reporting is a separate concern
 * and lives in `core:observability`, which forwards what it needs to Crashlytics.
 */
interface Logger {
    fun debug(
        tag: String,
        message: String,
    )

    fun info(
        tag: String,
        message: String,
    )

    fun warn(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    )

    fun error(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    )
}

/**
 * Writes to whatever the platform calls a console: logcat, stdout, or the browser console.
 *
 * A factory function rather than an `expect class`: an expect class that names a supertype has to
 * redeclare every member of it, and each actual has to repeat them again with `actual override` —
 * three copies of a signature list that buys nothing over returning the interface.
 */
expect fun platformLogger(): Logger
