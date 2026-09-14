import com.gmail.volkovskiyda.abit.buildlogic.AbitVersioning
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.abit.versioning)
}

// Read at the project level on purpose: inside `compose.desktop { }` the nested blocks are
// extension-aware themselves, so a `the<AbitVersioning>()` in there looks up the wrong container.
val abitVersioning = the<AbitVersioning>()

kotlin {
    jvmToolchain(
        libs.versions.jvmToolchain
            .get()
            .toInt(),
    )
}

dependencies {
    implementation(projects.app.shared)

    // Same reason as app:web: a plain JVM module gets no Koin BoM from the convention plugins, and
    // the koin-compose artifacts carry no version of their own.
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(compose.desktop.currentOs)
    implementation(libs.compose.material3)
    implementation(libs.kotlinx.coroutines.swing)

    implementation(libs.compose.ui.toolingPreview)

    // Compose Multiplatform's own UI test, which renders through Skiko in-process — no emulator and
    // no display server, so it runs on any CI machine.
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(compose.desktop.currentOs)
    testImplementation(libs.kotlin.test)
}

compose.desktop {
    application {
        mainClass = "com.gmail.volkovskiyda.abit.MainKt"

        nativeDistributions {
            // Dmg only: macOS is the one desktop target ABit ships. Msi and Deb would be untested
            // artifacts nobody asked for.
            targetFormats(TargetFormat.Dmg)
            packageName = "ABit"
            // Never edited per release — the same git-derived numbers the Android apps report.
            packageVersion = abitVersioning.packageVersion
            description = "Change a bit — a multiplatform pomodoro"
            vendor = "Dmitriy Volkovskiy"

            // Modules the bundled JRE must keep: jlink strips everything the analysis does not see,
            // and both of these are reached reflectively — java.sql by Room's bundled SQLite and
            // jdk.unsupported by the Firebase Java SDK. Re-check with `suggestRuntimeModules`
            // whenever a dependency with native or reflective code is added.
            modules("java.sql", "jdk.unsupported")

            macOS {
                bundleID = "com.gmail.volkovskiyda.abit"
                dockName = "ABit"
                infoPlist {
                    // What makes this a menu-bar app: no Dock icon, no app menu bar, nothing in
                    // the app switcher. The tray icon is the only entry point.
                    extraKeysRawXml = "<key>LSUIElement</key><string>1</string>"
                }
            }
        }
    }
}
