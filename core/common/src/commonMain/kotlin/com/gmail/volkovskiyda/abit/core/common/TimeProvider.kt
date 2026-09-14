package com.gmail.volkovskiyda.abit.core.common

import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Wall-clock time, injected so a test can freeze or advance it. A pomodoro is defined by elapsed
 * time, so nothing in this app is allowed to call the clock directly.
 */
fun interface TimeProvider {
    fun now(): Instant
}

class SystemTimeProvider : TimeProvider {
    override fun now(): Instant = Clock.System.now()
}
