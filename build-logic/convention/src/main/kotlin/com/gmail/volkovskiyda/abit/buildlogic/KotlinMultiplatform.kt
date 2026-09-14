package com.gmail.volkovskiyda.abit.buildlogic

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

/**
 * The Android namespace every module derives from its Gradle path, so no module repeats it.
 * `:core:model` becomes `com.gmail.volkovskiyda.abit.core.model`, `:feature:pomodoro:impl` becomes
 * `com.gmail.volkovskiyda.abit.feature.pomodoro.impl`.
 */
internal val Project.derivedNamespace: String
    get() = "com.gmail.volkovskiyda.abit" + path.replace(":", ".").replace("-", "")

/**
 * Declares the three target platforms — Android, the desktop JVM and the browser — plus the
 * intermediate source sets that carry dependencies shared by some but not all of them.
 *
 * There is no iOS target: ABit ships an Android phone app, a Wear OS app, a macOS tray app and a
 * web app, and nothing else. Adding one later means adding it here, not in sixteen build scripts.
 *
 * Since AGP 9 the Android side of a multiplatform module comes from
 * `com.android.kotlin.multiplatform.library`, which contributes an `android` target to the `kotlin`
 * extension instead of a separate `android { }` block. Its test compilations are opt in, so
 * `withHostTest`/`withDeviceTest` are what create `androidHostTest` and `androidDeviceTest`.
 */
internal fun Project.configureKotlinMultiplatform(extension: KotlinMultiplatformExtension) =
    extension.apply {
        val jvmTargetVersion = libs.version("jvmTarget")

        androidLibraryTarget(this@configureKotlinMultiplatform, jvmTargetVersion)

        jvm("desktop") {
            compilerOptions { jvmTarget.set(JvmTarget.fromTarget(jvmTargetVersion)) }
        }

        wasmJs { browser() }

        applyDefaultHierarchyTemplate()

        compilerOptions {
            freeCompilerArgs.addAll("-Xexpect-actual-classes")
        }

        with(sourceSets) {
            all {
                languageSettings.optIn("kotlin.RequiresOptIn")
                languageSettings.optIn("kotlin.time.ExperimentalTime")
            }

            // Everything Skiko renders: no Android framework, no `android.content.Context`.
            // `dependOn` skips names that do not exist, so a target left out of this list silently
            // loses every actual declared here.
            val nonAndroidMain = create("nonAndroidMain") { dependsOn(getByName("commonMain")) }
            dependOn(nonAndroidMain, "desktopMain", "wasmJsMain")

            // The test counterpart. Room's bundled SQLite ships a JNI library that an Android
            // *host* unit test cannot load, so tests touching a real database live here and run on
            // the desktop JVM instead.
            val nonAndroidTest = create("nonAndroidTest") { dependsOn(getByName("commonTest")) }
            dependOn(nonAndroidTest, "desktopTest")

            // Android and desktop share a JVM runtime, so they share JVM-only libraries.
            val jvmSharedMain = create("jvmSharedMain") { dependsOn(getByName("commonMain")) }
            dependOn(jvmSharedMain, "androidMain", "desktopMain")
        }
    }

/**
 * Wires [parent] into each named source set that exists. Test source sets only materialise once
 * `withHostTest`/`withDeviceTest` have run, so a missing name is skipped rather than failing.
 */
private fun NamedDomainObjectContainer<KotlinSourceSet>.dependOn(
    parent: KotlinSourceSet,
    vararg names: String,
) = names.forEach { name -> findByName(name)?.dependsOn(parent) }

private fun KotlinMultiplatformExtension.androidLibraryTarget(
    project: Project,
    jvmTargetVersion: String,
) {
    val android = (this as ExtensionAware).extensions
        .getByName("android") as KotlinMultiplatformAndroidLibraryTarget

    android.apply {
        namespace = project.derivedNamespace
        compileSdk = project.libs.version("androidCompileSdk").toInt()
        minSdk = project.libs.version("androidMinSdk").toInt()
        compilerOptions { jvmTarget.set(JvmTarget.fromTarget(jvmTargetVersion)) }
        androidResources.enable = true

        lint { configureAbitLint(this) }

        withHostTest {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
        withDeviceTest {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }
}
