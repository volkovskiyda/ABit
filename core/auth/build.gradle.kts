plugins {
    alias(libs.plugins.abit.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.domain)
            api(projects.core.common)
            api(libs.gitlive.firebase.auth)
            implementation(libs.gitlive.firebase.app)
        }
        androidMain.dependencies {
            // Google sign-in on Android goes through Credential Manager rather than the retired
            // Google Sign-In SDK: it is the only path that still works on Android 14+ and it is what
            // Wear OS offers too.
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.playServicesAuth)
            implementation(libs.googleid)
            implementation(libs.koin.android)
        }
        desktopMain.dependencies {
            // GitLive reaches the JVM through a port of the Firebase Android SDK, which needs a
            // storage and logging implementation supplied before anything else touches Firebase.
            implementation(libs.gitlive.firebase.java.sdk)
        }
        desktopTest.dependencies {
            // The Firebase Emulator Suite harness, for the Google sign-in this module implements
            // itself on the JVM.
            implementation(projects.core.testing)
        }
    }
}
