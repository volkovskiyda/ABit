plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

// Deliberately not the abit.kmp.library convention plugin: that one declares Android and desktop
// targets too, and this module is a browser executable with exactly one target.
kotlin {
    wasmJs {
        browser {
            commonWebpackConfig { outputFileName = "abit.js" }
        }
        binaries.executable()
    }

    sourceSets {
        wasmJsMain.dependencies {
            implementation(projects.app.shared)

            // This module deliberately skips the abit.kmp.library convention plugin, so it also
            // misses the Koin BoM that plugin applies — and the koin-compose artifacts below carry
            // no version of their own.
            implementation(project.dependencies.platform(libs.koin.bom))

            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(projects.core.designsystem)

            // The tablet layout is the web layout: a navigation rail and list-detail panes, degrading
            // to a bottom bar in a narrow browser window for free.
            implementation(libs.compose.adaptive)
            implementation(libs.compose.adaptive.layout)
            implementation(libs.compose.adaptive.navigation)
            implementation(libs.compose.material3.adaptive.navigationSuite)

            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.kotlinx.browser)
        }
    }
}
