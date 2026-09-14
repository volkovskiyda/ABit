plugins {
    alias(libs.plugins.abit.kmp.library)
    alias(libs.plugins.abit.versioning)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.common)
            api(projects.core.model)
            api(projects.core.domain)
            api(projects.core.data)
            api(projects.core.observability)
            api(projects.feature.pomodoro.impl)

            implementation(projects.core.auth)
            implementation(projects.core.database)
            implementation(projects.core.datastore)
            implementation(projects.core.sync)
        }
    }
}
