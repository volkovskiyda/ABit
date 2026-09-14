plugins {
    alias(libs.plugins.abit.kmp.library)
}

kotlin {
    sourceSets {
        // Deliberately thin: navigation keys and the types a host needs to reach this feature, and
        // nothing else. Another feature may depend on this module; none may depend on :impl.
        commonMain.dependencies {
            api(projects.core.model)
            api(libs.androidx.navigation3.runtime)
        }
    }
}
