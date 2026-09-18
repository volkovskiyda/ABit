package com.gmail.volkovskiyda.abit.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.TestExtension
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
                // 720 × 1280 at xhdpi — 360 × 640 dp, the shortest screen anything in the Test Lab
                // matrix runs on, and a hundred dp shorter than the Pixel above. Height is the axis
                // this suite gets wrong: every screen is one scrolling column, and an assertion that
                // a section is displayed passes on a tall device whether or not the app would scroll
                // to it. Test Lab caught exactly that on SmallPhone.arm, but Test Lab runs on main
                // pushes only — this is the same question asked on every pull request.
                create(SMALL_DEVICE) {
                    device = "Small Phone"
                    apiLevel = TEST_API_LEVEL
                    systemImageSource = "aosp-atd"
                }
            }
            groups {
                // `./gradlew ciGroupDebugAndroidTest` is what CI runs, so adding a device to the
                // matrix is a change here rather than a change to a workflow file.
                create("ci") {
                    targetDevices.add(localDevices.getByName(ATD_DEVICE))
                    targetDevices.add(localDevices.getByName(SMALL_DEVICE))
                }
            }
        }
    }
}

/**
 * The Wear app needs a round screen and a Wear system image; a phone device would not even boot.
 *
 * Deliberately **not** in the `ci` group. A GitHub runner cannot install
 * `system-images;android-34;android-wear;x86_64` — the setup task fails with
 * `InstallFailedException: Failed to install the following SDK components` (measured 2026-09-14),
 * because Wear images are not among those AGP can fetch unattended. So CI runs the phone app's
 * instrumented suite, which shares the entire object graph with the watch, and the Wear device stays
 * available locally:
 *
 * ```sh
 * ./gradlew :app:wear:wearLargeRoundApi34DebugAndroidTest
 * ```
 *
 * Getting it into CI needs the image installed explicitly before Gradle runs — a backlog item, not a
 * thing to leave silently failing.
 */
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
        }
    }
}

/**
 * The same devices again, for a `com.android.test` module.
 *
 * Declaring them twice is not redundancy: a managed device belongs to the module whose tasks run it,
 * and `:baselineprofile` runs its own. The baseline-profile plugin checks the name against *this*
 * module's devices and fails configuration if it is missing.
 */
internal fun TestExtension.configureManagedDevices() {
    testOptions {
        managedDevices {
            localDevices {
                create(ATD_DEVICE) {
                    device = "Pixel 6"
                    apiLevel = TEST_API_LEVEL
                    systemImageSource = "aosp-atd"
                }
                // What the profile is generated on: ART on an ATD image will not emit one.
                create(FULL_DEVICE) {
                    device = "Pixel 6"
                    apiLevel = TEST_API_LEVEL
                    systemImageSource = "aosp"
                }
            }
        }
    }
}

internal const val ATD_DEVICE = "pixel6Api35Atd"
internal const val SMALL_DEVICE = "smallPhoneApi35Atd"
internal const val FULL_DEVICE = "pixel6Api35"
internal const val WEAR_DEVICE = "wearLargeRoundApi34"
private const val TEST_API_LEVEL = 35
private const val WEAR_API_LEVEL = 34
