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
        commonTest.dependencies {
            // The fakes and the frozen clock. No cycle: core:testing depends on core:domain, not on
            // this module.
            implementation(projects.core.testing)
        }
    }
}
