package com.gmail.volkovskiyda.abit.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Project

/**
 * Fixes up the build types the baseline-profile plugin generates: `nonMinifiedRelease`, which the
 * generator runs against, and `benchmarkRelease`, which the macrobenchmarks measure.
 *
 * Applied in `finalizeDsl` because the plugin creates those build types after this project's own
 * configuration has run, so nothing earlier can see them.
 */
internal fun Project.configureBenchmarkVariants(android: ApplicationExtension) {
    android.buildTypes
        .filter { it.name.startsWith("nonMinified") || it.name.startsWith("benchmark") }
        .forEach { buildType ->
            // Neither profiling variant may share the shipped app's application id. If it did, a
            // generation run would install *over* the release build on the device and the
            // connected-test teardown would then uninstall it, taking that install's data with it.
            // The suffix costs a client entry in google-services.json — the Google Services plugin
            // fails with "No matching client found for package name" without one, which is why the
            // Firebase project has an "ABit Benchmark" app — and it cannot reach the profile, which
            // is a list of classes and methods in a namespace that does not change.
            buildType.applicationIdSuffix = ".benchmark"
            buildType.versionNameSuffix = "-benchmark"
        }

    android.buildTypes.filter { it.name.startsWith("nonMinified") }.forEach { buildType ->
        // The generator must see readable class names. The plugin only clears the legacy
        // `isMinifyEnabled` flag, which this project never sets — R8 is switched on through the
        // release build type's `optimization` block — so without this the first profile comes out
        // obfuscated and silently useless.
        buildType.optimization.enable = false

        // Signing is left exactly as inherited from release, so the generator profiles a build
        // signed the way the shipped one is, and only filled in when that inheritance yields
        // nothing. It yields nothing on a checkout with no keystore.properties, where release's
        // config is never created: the APK would come out unsigned, the device would refuse to
        // install it, and generating a profile would be impossible for anyone but the keystore
        // holder. Signing cannot reach the profile either way.
        if (buildType.signingConfig == null) {
            buildType.signingConfig = android.signingConfigs.getByName("debug")
        }
    }
}
