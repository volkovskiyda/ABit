package com.gmail.volkovskiyda.abit.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test

/**
 * Records which classes and methods the app actually runs on the way to its first screen, so ART can
 * compile them ahead of time on a user's device instead of interpreting them on first launch.
 *
 * Run it deliberately — `./gradlew :app:android:generateReleaseBaselineProfile`, or the
 * baseline-profile workflow — and commit the result. It is not part of a release build: a release
 * that needed an emulator could not be built in CI.
 *
 * The journey is short on purpose. It should cover what every launch does, and no more: a profile
 * that covers a path most users never take spends install-time compilation on nothing.
 */
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun startup() =
        baselineProfileRule.collect(
            // The application id comes from the build (see this module's build.gradle.kts): the
            // profiling variants carry a `.benchmark` suffix, so it is not the shipped id.
            packageName = targetAppId,
            // Startup gets its own, smaller profile that ART applies before anything else runs.
            includeInStartupProfile = true,
        ) {
            pressHome()
            startActivityAndWait()
            // Waiting on real content rather than on the window: without this the profile stops at the
            // splash screen and misses everything Compose, Koin and Room do to render the first frame.
            // "Today" is the first screen's title and its navigation label, and it is on the first
            // frame — "ABit" used to be here and no longer appears anywhere in the app.
            device.wait(Until.hasObject(By.text("Today")), CONTENT_TIMEOUT_MILLIS)

            // Past startup: the screens a user actually reaches. Without this the profile covers the
            // first frame and nothing else, and the Schedules list is where Room and the planner do
            // their real work.
            device.findObject(By.text("Schedules"))?.click()
            device.wait(Until.hasObject(By.text("New schedule")), CONTENT_TIMEOUT_MILLIS)
            device.findObject(By.text("Settings"))?.click()
            device.wait(Until.hasObject(By.text("APPEARANCE")), CONTENT_TIMEOUT_MILLIS)
        }

    private companion object {
        val targetAppId: String
            get() =
                requireNotNull(
                    InstrumentationRegistry.getArguments().getString("targetAppId"),
                ) { "targetAppId was not passed — see :baselineprofile's build.gradle.kts" }

        const val CONTENT_TIMEOUT_MILLIS = 10_000L
    }
}
