plugins {
    alias(libs.plugins.composeScreenshot)
    alias(libs.plugins.baselineprofile)
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

    // Paired with the same flag in gradle.properties; the plugin is still alpha and gated on both.
    experimentalProperties["android.experimental.enableScreenshotTest"] = true

    defaultConfig {
        applicationId = "com.gmail.volkovskiyda.abit"
    }
}

// LayoutLib does not round antialiasing identically on every host. The goldens are baked on an
// arm64 Mac and validated on an x86_64 Linux runner, and two pixels on the Schedules screen come out
// one 255th apart there — 2 pixels in 2,592,000. At the differ's default of exact equality that is a
// red build for a difference no eye can find, on the two screens that happen to carry the element
// whose antialiased edge lands on the unlucky pixel.
//
// The value is a *fraction* of differing pixels, not a percentage: PixelPerfect compares
// `differing / (width * height)` against it and fails on strictly greater. 0.00001 is 25 of this
// image's pixels: twelve times the rounding actually observed, and still an order of magnitude below
// the smallest regression worth catching — one changed digit in a time label is a couple of hundred
// pixels, and a moved row or a wrong palette is thousands.
//
// Picking a rounder 0.0001 would allow 259, which is the same size as that single changed digit. The
// number has to sit in the gap between the two, not merely above the noise.
//
// This is a tolerance for a comparison that is genuinely analog, not a baseline: no finding is being
// recorded and forgiven, and the number has to stay this small for that to remain true.
//
// Set on the task because alpha16 of the plugin exposes no DSL for it — `testOptions.screenshotTests`
// carries only the engine version and the target variants.
// A `val`, not a `const val`: a .gradle.kts file compiles to a class body, where const is illegal.
val hostAntialiasingTolerance = 0.00001f

tasks.withType<com.android.compose.screenshot.tasks.PreviewScreenshotValidationTask>().configureEach {
    testEngineInput.threshold.set(hostAntialiasingTolerance)
}

// A release build must never need a device. Generation is a deliberate step — `./gradlew
// :app:android:generateReleaseBaselineProfile`, or the baseline-profile workflow — with its output
// committed, so assembleRelease just packages whatever is checked in. Left at its default `true`,
// assembleRelease would try to boot an emulator and CI would fail.
baselineProfile {
    automaticGenerationDuringBuild = false
}

dependencies {
    implementation(projects.app.shared)
    implementation(projects.core.designsystem)
    implementation(libs.compose.adaptive)
    implementation(libs.compose.adaptive.layout)
    implementation(libs.compose.adaptive.navigation)
    implementation(libs.compose.material3.adaptive.navigationSuite)
    // Installs the packaged profile on first run for devices that do not do it themselves.
    implementation(libs.androidx.profileinstaller)
    baselineProfile(projects.baselineprofile)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Google sign-in. Credential Manager is the only supported path on Android 14+; the googleid
    // artifact is what turns its response into an id token Firebase can exchange.
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.playServicesAuth)
    implementation(libs.googleid)

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

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    // Runs an accessibility audit as part of an ordinary assertion, so a contrast or touch-target
    // regression fails the same suite that catches a layout one.
    androidTestImplementation(libs.androidx.compose.ui.test.junit4.accessibility)
    androidTestImplementation(libs.androidx.test.ext.junit)
    // Declared only to lift the version Compose ui-test asks for — see the catalog comment.
    androidTestImplementation(libs.androidx.test.espresso.core)
    debugImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Renders @Preview composables through LayoutLib and diffs them against committed PNGs, so a
    // layout regression shows up as an image diff rather than as nobody noticing.
    screenshotTestImplementation(libs.compose.ui.tooling)
    screenshotTestImplementation(libs.screenshot.validation.api)
}
