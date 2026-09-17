plugins {
    alias(libs.plugins.abit.kmp.feature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.settings.api)
            // The per-device chime settings live in DataStore, not in Firestore.
            api(projects.core.datastore)
            // A switch that turns chiming or vibration on rings once, so the user hears what it did.
            api(projects.core.chime)
        }
    }
}
