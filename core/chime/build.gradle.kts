plugins {
    alias(libs.plugins.abit.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.domain)
            api(projects.core.common)
            api(projects.core.datastore)
        }
        commonTest.dependencies {
            // The fakes and the frozen clock. No cycle: core:testing stops at core:domain.
            implementation(projects.core.testing)
        }
    }
}
