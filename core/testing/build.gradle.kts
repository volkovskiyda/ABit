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
        // The Firebase Emulator Suite harness. Desktop only, because the emulators are driven from
        // a JVM test, and in `desktopMain` rather than `desktopTest` for the same reason everything
        // else here is in `commonMain`: a module's test source set is not visible to any other
        // module. It needs no Firebase dependency — it speaks the emulator's REST API over the JDK's
        // own HTTP client, which is what lets it test the security rules at all (see Emulator).
    }
}
