plugins {
    alias(libs.plugins.abit.kmp.feature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.settings.api)
            // The per-device chime settings live in DataStore, not in Firestore.
            api(projects.core.datastore)
        }
    }
}
