plugins {
    alias(libs.plugins.abit.kmp.feature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.today.api)
            // The sync badge and the conflict sheet send the user into Schedules. An `:api` dependency
            // only — a feature never compiles against another feature's implementation.
            api(projects.feature.schedules.api)
            api(projects.feature.settings.api)
        }
    }
}
