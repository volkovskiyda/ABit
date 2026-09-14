import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":sharedLogic"))

    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "com.gmail.volkovskiyda.abit.MainKt"

        nativeDistributions {
            // Dmg only: macOS is the one desktop target ABit ships (plan item 04 turns this into a
            // menu-bar tray app). Msi and Deb would be untested artifacts nobody asked for.
            targetFormats(TargetFormat.Dmg)
            packageName = "ABit"
            packageVersion = "1.0.0"
        }
    }
}
