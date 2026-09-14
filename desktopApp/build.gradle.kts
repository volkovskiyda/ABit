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
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())
}

dependencies {
    implementation(projects.app.shared)

    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(libs.kotlinx.coroutines.swing)

    implementation(libs.compose.ui.toolingPreview)
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
        }
    }
}
