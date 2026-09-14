plugins {
    alias(libs.plugins.abit.android.application)
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
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.runtime.compose)

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
