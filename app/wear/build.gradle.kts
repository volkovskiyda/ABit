plugins {
    alias(libs.plugins.abit.android.application)
}

android {
    namespace = "com.gmail.volkovskiyda.abit.wear"

    defaultConfig {
        // The same application id as the phone app, which is the Wear convention: the two are one
        // product to the Play Store and to Firebase, so they also share one google-services client.
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

    // Wear has its own Material 3, sized and shaped for a round screen — the phone's material3 is
    // the wrong component set here, not merely a different theme.
    implementation(libs.wear.compose.material3)
    implementation(libs.wear.compose.foundation)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)

    implementation(libs.wear.toolingPreview)
    implementation(libs.compose.ui.toolingPreview)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.wear.compose.uiTooling)
}
