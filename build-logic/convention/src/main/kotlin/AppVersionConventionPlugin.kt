import com.gmail.volkovskiyda.abit.buildlogic.AbitVersioning
import com.gmail.volkovskiyda.abit.buildlogic.GenerateAppVersionTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Bakes the version name into a `commonMain` constant for the module that applies this, which is
 * `app:shared` and only `app:shared` — the one place all four apps share, and therefore the one
 * place the number has to exist. See [GenerateAppVersionTask] for why it is generated.
 *
 * Applied by hand rather than folded into [KmpLibraryConventionPlugin]: every `core:*` and
 * `feature:*` module uses that one, and none of them has any business knowing the release number.
 */
class AppVersionConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("abit.versioning")

            // Read outside the register block on purpose: a Gradle task is itself ExtensionAware,
            // so `the<AbitVersioning>()` in there resolves against the task and finds nothing.
            val versioning = the<AbitVersioning>()

            val generate =
                tasks.register<GenerateAppVersionTask>("generateAppVersion") {
                    description = "Writes the git-derived version name into a Kotlin constant."
                    versionName.set(versioning.versionName)
                    packageName.set("com.gmail.volkovskiyda.abit.app.shared")
                    outputDirectory.set(layout.buildDirectory.dir("generated/abitVersion/kotlin"))
                }

            // flatMap rather than the task provider itself: it hands the source set both the
            // directory and the edge to the task that fills it, for every target's compilation.
            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.named("commonMain") {
                    kotlin.srcDir(generate.flatMap { it.outputDirectory })
                }
            }

            // AGP's lint tasks read the source directories without declaring who writes them — the
            // same edge KmpLibraryConventionPlugin adds for KSP, and Gradle 9 fails rather than
            // warns.
            tasks.matching {
                (it.name.startsWith("generate") && it.name.endsWith("LintModel")) ||
                    it.name.startsWith("lintAnalyze") ||
                    it.name.startsWith("lintVitalAnalyze")
            }.configureEach { dependsOn(generate) }
        }
    }
}
