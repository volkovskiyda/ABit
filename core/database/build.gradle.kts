plugins {
    alias(libs.plugins.abit.kmp.library)
    alias(libs.plugins.room)
    alias(libs.plugins.ksp)
}

// Checked into core/database/schemas so a version bump can ship a real migration instead of
// guessing what the previous schema was.
room3 {
    schemaDirectory("$projectDir/schemas")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.model)
            api(projects.core.common)
            api(libs.androidx.room.runtime)
        }
        // The SQLite driver is per platform and there is no common default: Android and the desktop
        // JVM link the bundled native library, while the browser has no SQLite at all and talks to
        // a Web Worker running the official WASM build (see app/web's static resources).
        androidMain.dependencies {
            implementation(libs.androidx.sqlite.bundled)
            // androidContext(): the Room builder needs a Context, which only this target has.
            implementation(libs.koin.android)
        }
        desktopMain.dependencies {
            implementation(libs.androidx.sqlite.bundled)
        }
        wasmJsMain.dependencies {
            implementation(libs.androidx.sqlite.web)
            implementation(libs.kotlinx.browser)
        }
        // Room's bundled SQLite ships a JNI library that an Android *host* unit test cannot load, so
        // tests that touch a real database live in nonAndroidTest and run on the desktop JVM. The
        // Android instrumented twin covers the same ground on a device.
        nonAndroidTest.dependencies {
            implementation(libs.androidx.sqlite.bundled)
        }
    }
}

dependencies {
    // One KSP invocation per target that compiles Room's generated code. Listing them explicitly is
    // required: a bare `ksp(...)` configures none of a multiplatform module's compilations.
    listOf("kspAndroid", "kspDesktop", "kspWasmJs").forEach { configuration ->
        add(configuration, libs.androidx.room.compiler)
    }
}
