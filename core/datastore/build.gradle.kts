plugins {
    alias(libs.plugins.abit.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.model)
            api(projects.core.common)
            api(libs.androidx.datastore.core.okio)
            implementation(libs.okio)
        }
        androidMain.dependencies {
            // androidContext(): the preferences file lives under the app's own filesDir.
            implementation(libs.koin.android)
        }
        wasmJsMain.dependencies {
            implementation(libs.kotlinx.browser)
        }
        commonTest.dependencies {
            implementation(libs.okio.fakefilesystem)
        }
    }
}
