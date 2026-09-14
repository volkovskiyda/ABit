plugins {
    alias(libs.plugins.abit.kmp.feature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.pomodoro.api)
        }
    }
}
