plugins {
    // Before the convention plugin: Kotzilla adjusts Kotlin compiler options, and the Kotlin
    // extension the convention plugin configures finalises them.
    alias(libs.plugins.kotzilla)
    alias(libs.plugins.abit.kmp.library)
    alias(libs.plugins.abit.versioning)
}

// Kotzilla reads its API keys from app/shared/kotzilla.json, which is git-ignored because those keys
// are ingestion credentials and this repository is public (.example.kotzilla.json is the template,
// and CI restores the real file from a secret). The plugin fails configuration when the file is
// missing, so a checkout without one switches the whole thing off instead: a fresh clone, a fork's
// pull request and CI's build and test jobs all still build — exactly the way the release signing
// config is simply not created without keystore.properties.
//
// Everything else is left at the plugin's defaults on purpose. A line in this block is a decision to
// own. One that must never be set in a committed file is displayLogs: it turns on runtime logging
// that prints bearer tokens.
//
// Compose instrumentation being on is what ties this project to Kotlin 2.4.10: it is a Kotlin
// compiler plugin, and 2.3.6 still registers it through the K1 ComponentRegistrar interface that
// Kotlin 2.4.20 deleted. See the kotlin ref in gradle/libs.versions.toml before bumping either.
val kotzillaConfig = file("kotzilla.json")

kotzilla {
    enabled = kotzillaConfig.exists()
    // A shared module has no Android versionName for the plugin to read, so the reported version is
    // set explicitly — from the same git-derived numbers the apps use.
    versionName = the<com.gmail.volkovskiyda.abit.buildlogic.AbitVersioning>().versionName
}

kotlin {
    sourceSets {
        // The plugin is switched off when kotzilla.json is missing, and a disabled plugin generates
        // no `monitoring()` at all. A keyless checkout therefore compiles hand-written no-ops
        // (src/kotzillaDisabled); a keyed one compiles the real calls (src/kotzillaEnabled) beside
        // the generated monitoring(). Since no build compiles both, only running both proves the
        // two stay signature-identical — CI's keyless jobs cover one side on every push.
        val seam = if (kotzillaConfig.exists()) "kotzillaEnabled" else "kotzillaDisabled"
        commonMain.get().kotlin.srcDir("src/$seam/kotlin")

        desktopMain.dependencies {
            implementation(libs.gitlive.firebase.java.sdk)
        }

        commonMain.dependencies {
            api(projects.core.common)
            api(projects.core.model)
            api(projects.core.domain)
            api(projects.core.data)
            api(projects.core.observability)
            api(projects.feature.pomodoro.impl)

            implementation(projects.core.auth)
            implementation(libs.gitlive.firebase.app)
            implementation(projects.core.database)
            implementation(projects.core.datastore)
            implementation(projects.core.sync)
        }
    }
}
