import com.gmail.volkovskiyda.abit.buildlogic.configureConnectedTestGuard
import com.gmail.volkovskiyda.abit.buildlogic.configureKotlinMultiplatform
import com.gmail.volkovskiyda.abit.buildlogic.library
import com.gmail.volkovskiyda.abit.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Every shared module in this build: `core:*`, `feature:*:api` and `app:shared`. Declares the
 * targets, the Kotlin libraries that count as language-level here (coroutines, serialization,
 * datetime, Koin) and the test stack, so a module's own build script is a `plugins { }` block plus
 * the projects it depends on.
 */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.kotlin.multiplatform.library")
            pluginManager.apply("org.jetbrains.kotlin.multiplatform")
            pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

            extensions.configure<KotlinMultiplatformExtension> { configureKotlinMultiplatform(this) }

            dependencies {
                // The Koin BoM on both the main and test compilations, so no Koin artifact below
                // carries a version and they can never drift apart. On `api` rather than
                // `implementation`: koin-core is exposed as api, and a consuming module resolving
                // it needs the BoM's constraints too, or it sees a versionless coordinate.
                add("commonMainApi", platform(libs.library("koin-bom")))
                add("commonTestImplementation", platform(libs.library("koin-bom")))

                add("commonMainApi", libs.library("kotlinx-coroutines-core"))
                add("commonMainApi", libs.library("kotlinx-datetime"))
                add("commonMainImplementation", libs.library("kotlinx-serialization-json"))
                add("commonMainImplementation", libs.library("kotlinx-collections-immutable"))
                add("commonMainApi", libs.library("koin-core"))

                add("commonTestImplementation", libs.library("kotlin-test"))
                add("commonTestImplementation", libs.library("kotlinx-coroutines-test"))
                add("commonTestImplementation", libs.library("turbine"))
                add("commonTestImplementation", libs.library("koin-test"))
            }

            configureConnectedTestGuard()

            // AGP's lint tasks read KSP's generated source directories but do not declare the
            // dependency. Gradle 9 fails the build on that rather than warning, so the edge is
            // added here for whichever KSP tasks a module happens to register.
            tasks.matching {
                (it.name.startsWith("generate") && it.name.endsWith("LintModel")) ||
                    it.name.startsWith("lintAnalyze") ||
                    it.name.startsWith("lintVitalAnalyze")
            }.configureEach {
                dependsOn(tasks.matching { task -> task.name.startsWith("ksp") })
            }
        }
    }
}
