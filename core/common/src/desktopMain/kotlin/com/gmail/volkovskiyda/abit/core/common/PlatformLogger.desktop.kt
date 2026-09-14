package com.gmail.volkovskiyda.abit.core.common

actual fun platformLogger(): Logger = DesktopLogger

private object DesktopLogger : Logger {
    override fun debug(
        tag: String,
        message: String,
    ) = write("D", tag, message, null)

    override fun info(
        tag: String,
        message: String,
    ) = write("I", tag, message, null)

    override fun warn(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) = write("W", tag, message, throwable)

    override fun error(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) = write("E", tag, message, throwable)

    private fun write(
        level: String,
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        println("$level/$tag: $message")
        throwable?.printStackTrace()
    }
}
