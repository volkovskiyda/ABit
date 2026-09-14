// Top-level build file. Every third-party Gradle plugin this build uses is declared here with
// `apply false`, which puts it on the build classpath exactly once: the convention plugins in
// build-logic then apply it by id, without a version, and no subproject loads a second copy into
// its own classloader. Later plan items add their plugins to this list as they arrive (Room,
// Kotzilla, the Firebase trio, baseline profile, screenshot tests, detekt and ktlint).
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidKotlinMultiplatformLibrary) apply false
    alias(libs.plugins.androidTest) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.ksp) apply false
}
