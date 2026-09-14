package com.gmail.volkovskiyda.abit.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.kotlin.dsl.invoke

/**
 * Emulators Gradle downloads, boots and throws away on its own, so the instrumented suite runs
 * identically on a laptop and on a CI runner with nobody wiring up an AVD.
 *
 * ABit can use these where Jellyshelf could not: nothing here is arm64-only, so an x86_64 runner can
 * install the APK, which is what makes the instrumented layer affordable on every pull request
 * instead of only on Firebase Test Lab.
 */
internal fun ApplicationExtension.configureManagedDevices() {
    testOptions {
        managedDevices {
            localDevices {
                // An Automated Test Device: no Play services, no animations, and a stripped system
                // image that boots in a fraction of the time. The right default for a test suite
                // that only needs the app and the framework.
                create(ATD_DEVICE) {
                    device = "Pixel 6"
                    apiLevel = TEST_API_LEVEL
                    systemImageSource = "aosp-atd"
                }
                // The same device on a full AOSP image. ART on an ATD image will not produce a
                // baseline profile, so profile generation (see :baselineprofile) needs this one.
                create(FULL_DEVICE) {
                    device = "Pixel 6"
                    apiLevel = TEST_API_LEVEL
                    systemImageSource = "aosp"
                }
            }
            groups {
                // `./gradlew ciGroupDebugAndroidTest` is what CI runs, so adding a device to the
                // matrix is a change here rather than a change to a workflow file.
                create("ci") {
                    targetDevices.add(localDevices.getByName(ATD_DEVICE))
                }
            }
        }
    }
}

/** The Wear app needs a round screen and a Wear system image; a phone device would not even boot. */
internal fun ApplicationExtension.configureWearManagedDevices() {
    testOptions {
        managedDevices {
            localDevices {
                create(WEAR_DEVICE) {
                    device = "Wear OS Large Round"
                    // Wear OS 5. There is no ATD image for Wear, so this is a full one.
                    apiLevel = WEAR_API_LEVEL
                    systemImageSource = "android-wear"
                }
            }
            groups {
                create("ci") {
                    targetDevices.add(localDevices.getByName(WEAR_DEVICE))
                }
            }
        }
    }
}

internal const val ATD_DEVICE = "pixel6Api35Atd"
internal const val FULL_DEVICE = "pixel6Api35"
internal const val WEAR_DEVICE = "wearLargeRoundApi34"
private const val TEST_API_LEVEL = 35
private const val WEAR_API_LEVEL = 34
