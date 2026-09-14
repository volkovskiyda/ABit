import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}
dependencies {
    implementation(project(":sharedLogic"))

    implementation(libs.androidx.activity.compose)

    // Declared here rather than inherited from a shared UI module: UI is written per platform, so
    // each app names the Compose artifacts it actually uses. These are the Compose Multiplatform
    // coordinates, which resolve to their Android variants in an Android application module.
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "com.gmail.volkovskiyda.abit"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.gmail.volkovskiyda.abit"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    signingConfigs {
        // A project-local debug keystore, committed with the standard debug credentials, instead of
        // the per-machine ~/.android/debug.keystore AGP would otherwise generate. It gives this
        // laptop, any contributor and CI the same debug SHA-1 — which is what the Firebase Android
        // API key restriction gets pinned to in plan item 08. The shared ~/.android one is left
        // alone: other projects' installed debug builds depend on it.
        getByName("debug") {
            storeFile = rootProject.file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}
