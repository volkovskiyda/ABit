plugins {
    alias(libs.plugins.abit.kmp.library)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

// Compose is configured here rather than in a convention plugin, following `app/web` and
// `app/desktop`: CLAUDE.md asks for a convention plugin once configuration repeats in two modules,
// and this is the only Compose-enabled library. Promote it the moment a second one appears.
// The generated `Res` class lands in this module's own package rather than a derived one, so the
// import reads like the rest of the tree.
compose.resources {
    packageOfResClass = "com.gmail.volkovskiyda.abit.core.designsystem.generated.resources"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.model)
            api(projects.core.domain)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
        }
    }
}
