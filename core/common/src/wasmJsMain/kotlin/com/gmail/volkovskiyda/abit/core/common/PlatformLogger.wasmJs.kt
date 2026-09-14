package com.gmail.volkovskiyda.abit.core.common

actual class PlatformLogger actual constructor() : Logger {
    override fun debug(
        tag: String,
        message: String,
    ) {
        println("D/$tag: $message")
    }

    override fun info(
        tag: String,
        message: String,
    ) {
        println("I/$tag: $message")
    }

    override fun warn(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        println("W/$tag: $message${throwable?.let { " — $it" }.orEmpty()}")
    }

    override fun error(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        println("E/$tag: $message${throwable?.let { " — $it" }.orEmpty()}")
    }
}
