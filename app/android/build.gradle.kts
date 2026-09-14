plugins {
    // Before the convention plugin: Kotzilla adjusts Kotlin compiler options and the Kotlin
    // extension the convention plugin configures finalises them. It is applied here rather than at
    // the root project — see the comment on the root plugins block — and in this module rather than
    // only in app:shared because this is where the screens are: the plugin's Compose instrumentation
    // is what records screen views.
    alias(libs.plugins.kotzilla)
    alias(libs.plugins.abit.android.application)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.firebaseCrashlytics)
    alias(libs.plugins.firebasePerf)
}

// The key file lives with the composition root that calls monitoring(). Absent, the plugin disables
// itself and the app builds and runs exactly as before, reporting no sessions.
kotzilla {
    enabled = rootProject.file("app/shared/kotzilla.json").exists()
}

android {
    namespace = "com.gmail.volkovskiyda.abit"

    defaultConfig {
        applicationId = "com.gmail.volkovskiyda.abit"
    }
}

dependencies {
    implementation(projects.app.shared)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Crashlytics and Performance are Android-only by decision: the desktop and web builds bind the
    // no-op reporters in core:observability rather than take on a second vendor. The BoM pins both.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.perf)
    implementation(libs.androidx.tracing)

    // Declared here rather than inherited from a shared UI module: UI is written per platform, so
    // each app names the Compose artifacts it actually uses. These are the Compose Multiplatform
    // coordinates, which resolve to their Android variants in an Android application module.
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)

    implementation(libs.compose.ui.toolingPreview)
    debugImplementation(libs.compose.ui.tooling)
}
