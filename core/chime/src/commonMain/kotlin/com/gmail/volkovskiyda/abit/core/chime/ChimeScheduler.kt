package com.gmail.volkovskiyda.abit.core.chime

import com.gmail.volkovskiyda.abit.core.domain.Chime
import com.gmail.volkovskiyda.abit.core.domain.TodayState

/**
 * The one thing each platform has to implement for the app to make a sound.
 *
 * Exactly one chime is armed at a time, on every platform: Android's `AlarmManager` takes one, the
 * desktop sleeps until the first, and a browser tab has one timer. Re-arming after each firing is
 * what keeps the four implementations the same shape.
 */
interface ChimeScheduler {
    /** Arms the platform for [chime], replacing whatever was armed before. */
    suspend fun arm(chime: Chime)

    /** Nothing is scheduled: off hours, paused, or chiming is off on this device. */
    suspend fun disarm()

    /** Renders or clears the ongoing countdown, on the platforms that have one. */
    suspend fun showCountdown(state: TodayState)
}
