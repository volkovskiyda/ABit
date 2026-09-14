plugins {
    alias(libs.plugins.abit.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.domain)
            api(projects.core.common)
            api(projects.core.auth)
            api(libs.gitlive.firebase.firestore)
        }
        desktopTest.dependencies {
            // The Firebase Emulator Suite harness and the REST client the rules test drives.
            implementation(projects.core.testing)
        }
    }
}
