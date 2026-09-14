import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

// Top-level build file. Every third-party Gradle plugin this build uses is declared here with
// `apply false`, which puts it on the build classpath exactly once: the convention plugins in
// build-logic then apply it by id, without a version, and no subproject loads a second copy into
// its own classloader. Later plan items add their plugins to this list as they arrive (Room,
// Kotzilla, the Firebase trio, baseline profile, screenshot tests).
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidKotlinMultiplatformLibrary) apply false
    alias(libs.plugins.androidTest) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    // `apply false`, not applied here, and this is a measured constraint rather than a preference.
    // Applying Kotzilla at the root project fails configuration outright with "The value for
    // property 'languageVersion' is final and cannot be changed any further": it adjusts Kotlin
    // compiler options across the build, and the Kotlin extension the KMP convention plugin
    // configures has already finalised them by then (measured 2026-09-14, Kotzilla 2.3.6 with AGP
    // 9.4.0 and Kotlin 2.4.10). The vendor's own SDK guide documents per-module application as the
    // supported alternative, so the plugin is applied in app:shared, app:android and app:wear —
    // the shared module plus every module that owns screens, which is the coverage the root
    // application would have given. Re-test on a Kotzilla bump.
    alias(libs.plugins.kotzilla) apply false
    alias(libs.plugins.detekt)
    // Applied to every project below rather than here, so `apply false`.
    alias(libs.plugins.ktlint) apply false
}

// Static analysis for the whole repo, in three tools with no overlap between them. Android lint
// covers the platform-specific checks (configured once in build-logic's AndroidLint.kt); detekt
// covers Kotlin complexity, naming and style; ktlint below owns formatting.
//
// detekt can own formatting too, through the detekt-formatting plugin — which is ktlint's rule set
// wrapped, running whatever ktlint version detekt happens to embed. Running both would report every
// layout finding twice under two rule ids, from two engines free to disagree, so detekt-formatting
// is deliberately absent. Anything that looks like a missing formatting rule belongs in
// .editorconfig, not in detekt.yml.
detekt {
    // Every module's whole `src` tree — all source sets, all targets — plus the convention plugins,
    // which are Kotlin this project maintains like any other.
    source.from(
        files(subprojects.map { it.file("src") } + file("build-logic/convention/src")),
    )
    config.from(files("config/detekt/detekt.yml"))
    // The config file holds only this project's overrides; everything else comes from detekt's
    // defaults, so a version bump brings new rules instead of freezing a 500-line copy.
    buildUponDefaultConfig = true
    parallel = true
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    exclude("**/build/**", "**/generated/**")
    reports {
        // The HTML report is the one a human opens; the XML is what a summary task would parse.
        html.required.set(true)
        xml.required.set(true)
        sarif.required.set(false)
        md.required.set(false)
    }
}

// Formatting, applied to every project rather than pointed at source directories the way detekt is.
// The two plugins discover files differently: detekt takes a `source` set of paths, while ktlint
// walks each project's own Kotlin source sets (and, in an Android project, each variant's).
// Applying it only at the root would therefore lint the build scripts and not one line of module
// code. `allprojects` includes the root, which is how the scripts stay covered.
//
// `./gradlew ktlintCheck` runs the task in every project that has one, so the aggregate command
// stays a single word; `ktlintFormat` is the same set, fixing rather than reporting.
val ktlintVersion = libs.versions.ktlint.get()
allprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    extensions.configure<KtlintExtension> {
        // Pinned from the catalog. Left unset, the plugin picks its own default, which moves with
        // every plugin bump and takes the whole codebase's formatting with it.
        version.set(ktlintVersion)
        // No baseline and no tolerance, matching detekt's `maxIssues: 0`: a finding fails the
        // build. `ktlintFormat` fixes the great majority of them in place.
        ignoreFailures.set(false)
        // Rule ids alongside the message, so a finding says which rule to look up (or to configure
        // in .editorconfig) rather than only what it disliked.
        verbose.set(true)
        reporters {
            reporter(ReporterType.PLAIN)
            reporter(ReporterType.CHECKSTYLE)
            reporter(ReporterType.HTML)
        }
        filter {
            // Generated Kotlin is on the variant source sets AGP hands the plugin — KSP output and
            // Compose resource accessors — and none of it is ours to format.
            exclude { it.file.path.contains("${File.separator}build${File.separator}") }
        }
    }
}

// The Kotzilla plugin generates sources into a source set that other tasks then read as input, and
// Gradle 9 fails the build on an undeclared edge rather than warning. Two consumers need it:
// ktlint, which walks every Kotlin source directory a module has (it does not *lint* the generated
// file — the filter in the ktlint block above drops anything under build/ — but the directory is
// still an input), and KSP, which arrives with Room in a later plan item.
subprojects {
    tasks.matching { it.name.startsWith("ksp") || it.name.startsWith("runKtlint") }.configureEach {
        dependsOn(tasks.matching { it.name.startsWith("generateKotzilla") })
    }
}
