plugins {
    alias(libs.plugins.abit.kmp.library)
}

kotlin {
    sourceSets {
        androidMain.dependencies {
            // FirebaseApp.getApps(): the availability guard asks the platform SDK directly, because
            // it has to answer before Koin exists.
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.common)
        }
    }
}
