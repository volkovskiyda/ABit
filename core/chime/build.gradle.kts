plugins {
    alias(libs.plugins.abit.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.domain)
            api(projects.core.common)
            api(projects.core.datastore)
        }
        androidMain.dependencies {
            // NotificationCompat, ContextCompat.checkSelfPermission and getSystemService<T>().
            implementation(libs.androidx.core.ktx)
            // androidContext(): the scheduler needs a Context, which only this target has.
            implementation(libs.koin.android)
        }
        wasmJsMain.dependencies {
            // document.addEventListener("visibilitychange"), and the Notification API.
            implementation(libs.kotlinx.browser)
        }
        commonTest.dependencies {
            // The fakes and the frozen clock. No cycle: core:testing stops at core:domain.
            implementation(projects.core.testing)
        }
    }
}
