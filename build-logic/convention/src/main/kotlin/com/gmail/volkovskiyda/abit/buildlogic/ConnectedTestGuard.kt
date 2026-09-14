package com.gmail.volkovskiyda.abit.buildlogic

import org.gradle.api.Project
import org.gradle.api.provider.Provider

/**
 * Instrumented tests need a real device or emulator. Rather than failing a build that has none — CI
 * without an emulator job, a laptop with nothing plugged in — the `connected*AndroidTest` tasks skip
 * themselves and say so. Attach a device and the same command runs them for real.
 */
internal fun Project.configureConnectedTestGuard() {
    val adbPath: Provider<String> = providers.environmentVariable("ANDROID_HOME")
        .orElse(providers.environmentVariable("ANDROID_SDK_ROOT"))
        .map { "$it/platform-tools/adb" }
        .orElse("adb")

    tasks.matching { it.name.startsWith("connected") && it.name.endsWith("AndroidTest") }
        .configureEach {
            // Resolved at configuration time (a declared build input); the probe itself runs in the
            // onlyIf predicate, i.e. at execution time, so no build shells out to adb needlessly.
            val adb = adbPath.get()
            onlyIf {
                val attached = runCatching {
                    ProcessBuilder(adb, "devices").redirectErrorStream(true).start()
                        .inputStream.bufferedReader().readLines()
                        .drop(1) // "List of devices attached"
                        // Ignore "offline" and "unauthorized" — neither can run a test.
                        .any { line -> line.split(Regex("\\s+")).getOrNull(1) == "device" }
                }.getOrDefault(false)
                if (!attached) {
                    logger.lifecycle("No device or emulator attached — skipping $name.")
                }
                attached
            }
        }
}
