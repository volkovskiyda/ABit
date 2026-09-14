plugins {
    alias(libs.plugins.abit.kmp.library)
}

kotlin {
    sourceSets {
        // Everything this module offers lives in commonMain, not commonTest, so a consumer can
        // depend on it from its own test source sets. A commonTest here would not be publishable
        // to them at all.
        commonMain.dependencies {
            api(projects.core.common)
            api(projects.core.model)
            api(projects.core.domain)
            api(libs.kotlinx.coroutines.test)
        }
    }
}
