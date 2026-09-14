plugins {
    alias(libs.plugins.abit.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.domain)
            api(projects.core.common)
        }
    }
}
