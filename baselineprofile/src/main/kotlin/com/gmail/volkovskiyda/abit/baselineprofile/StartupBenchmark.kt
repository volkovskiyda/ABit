package com.gmail.volkovskiyda.abit.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test

/**
 * Cold-start time, measured twice: once with no ahead-of-time compilation and once with the
 * committed baseline profile applied. The pair is the point — the absolute numbers depend on the
 * device, but the difference between them is what the profile is worth, and the only way to know
 * whether it still earns its place after the startup path changes.
 *
 * Run on a real device for numbers worth quoting; an emulator's are too noisy to compare across
 * runs.
 */
class StartupBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startupWithoutProfile() = measure(CompilationMode.None())

    @Test
    fun startupWithProfile() =
        measure(
            // Require, not Enable: Require fails the run if the profile is missing rather than quietly
            // measuring the same thing as the test above and reporting no improvement.
            CompilationMode.Partial(baselineProfileMode = BaselineProfileMode.Require),
        )

    private fun measure(compilationMode: CompilationMode) =
        benchmarkRule.measureRepeated(
            packageName = targetAppId,
            metrics = listOf(StartupTimingMetric()),
            compilationMode = compilationMode,
            startupMode = StartupMode.COLD,
            iterations = ITERATIONS,
            setupBlock = { pressHome() },
        ) {
            startActivityAndWait()
            device.wait(Until.hasObject(By.text("ABit")), CONTENT_TIMEOUT_MILLIS)
        }

    private companion object {
        val targetAppId: String
            get() =
                requireNotNull(
                    InstrumentationRegistry.getArguments().getString("targetAppId"),
                ) { "targetAppId was not passed — see :baselineprofile's build.gradle.kts" }

        /** Enough for a stable median without making a full run tedious. */
        const val ITERATIONS = 5
        const val CONTENT_TIMEOUT_MILLIS = 10_000L
    }
}
