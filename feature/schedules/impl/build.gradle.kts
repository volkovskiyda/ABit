plugins {
    alias(libs.plugins.abit.kmp.feature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.schedules.api)
            // "Edit hours instead" on the conflict sheet returns to Today.
            api(projects.feature.today.api)
        }
    }
}
