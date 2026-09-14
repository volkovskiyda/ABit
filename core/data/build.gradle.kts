plugins {
    alias(libs.plugins.abit.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.domain)
            api(projects.core.database)
            implementation(projects.core.datastore)
            api(projects.core.sync)
            implementation(projects.core.auth)
        }
    }
}
