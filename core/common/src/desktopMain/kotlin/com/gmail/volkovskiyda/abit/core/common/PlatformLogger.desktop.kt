package com.gmail.volkovskiyda.abit.core.common

actual class PlatformLogger actual constructor() : Logger {
    override fun debug(
        tag: String,
        message: String,
    ) = print("D", tag, message, null)

    override fun info(
        tag: String,
        message: String,
    ) = print("I", tag, message, null)

    override fun warn(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) = print("W", tag, message, throwable)

    override fun error(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) = print("E", tag, message, throwable)

    private fun print(
        level: String,
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        println("$level/$tag: $message")
        throwable?.printStackTrace()
    }
}
