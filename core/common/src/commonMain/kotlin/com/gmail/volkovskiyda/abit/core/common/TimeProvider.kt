package com.gmail.volkovskiyda.abit.core.common

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Wall-clock time, injected so a test can freeze or advance it. Every chime is defined by elapsed
 * time, so nothing in this app is allowed to call the clock directly.
 */
fun interface TimeProvider {
    fun now(): Instant
}

class SystemTimeProvider : TimeProvider {
    override fun now(): Instant = Clock.System.now()
}

/**
 * The zone [Instant]s are read in, injected for the same reason [TimeProvider] is. A schedule is
 * local wall-clock plus weekday — 09:45 means 09:45 wherever the user is — so every conversion from
 * an instant to a date and a time goes through here, and a test can pin it to [TimeZone.UTC]
 * instead of depending on the machine it runs on.
 */
fun interface TimeZoneProvider {
    fun current(): TimeZone
}

class SystemTimeZoneProvider : TimeZoneProvider {
    override fun current(): TimeZone = TimeZone.currentSystemDefault()
}

/** The one place the two are combined, so no caller repeats the conversion. */
fun TimeProvider.localNow(zone: TimeZoneProvider): LocalDateTime = now().toLocalDateTime(zone.current())
