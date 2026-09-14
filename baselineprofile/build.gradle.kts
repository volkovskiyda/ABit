plugins {
    alias(libs.plugins.abit.android.test)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.gmail.volkovskiyda.abit.baselineprofile"
    targetProjectPath = ":app:android"
}

baselineProfile {
    // A Gradle-managed emulator rather than a connected device: nothing in this app is arm64-only,
    // so generation runs unattended — on this laptop and in CI — instead of being a manual step
    // somebody has to remember. The device is the full AOSP image, not the ATD one: ART on an ATD
    // image will not emit a profile.
    useConnectedDevices = false
    managedDevices += "pixel6Api35"
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.uiautomator)
}

// Which app to profile. The profiling variants carry a `.benchmark` application id suffix (see
// build-logic's configureBenchmarkVariants), so that is no longer the shipped id, and the generator
// reads it from this argument rather than holding a copy.
//
// The id comes off the built APK's own metadata rather than from `TestVariant.testedApplicationId`,
// which reports *this* module's id and would send the generator looking for an app that is not
// installed.
androidComponents {
    onVariants { variant ->
        val builtArtifacts = variant.artifacts.getBuiltArtifactsLoader()
        variant.instrumentationRunnerArguments.put(
            "targetAppId",
            variant.testedApks.map { apks ->
                requireNotNull(builtArtifacts.load(apks)?.applicationId) {
                    "No APK metadata under $apks — the app under test was not built."
                }
            },
        )
    }
}
