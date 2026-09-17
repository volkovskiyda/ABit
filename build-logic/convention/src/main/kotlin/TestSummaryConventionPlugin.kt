import com.gmail.volkovskiyda.abit.buildlogic.TestSummaryTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

/**
 * Registers `./gradlew testSummary` on the root project: one HTML page aggregating every test layer
 * and all three static-analysis tools.
 *
 * Applied to the root project only — the task is an aggregate over every module, so a per-module
 * copy would be twenty reports of one module each. It reads what is on disk and runs nothing, which
 * is what lets `scripts/run-tests.sh` call it after a failing layer instead of only after a green
 * run.
 */
class TestSummaryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        require(target == target.rootProject) {
            "abit.test-summary aggregates every module and belongs on the root project, not on ${target.path}."
        }
        with(target) {
            // Resolved at configuration time and handed over as plain files: the task must not hold
            // a Project reference, which is what the configuration cache forbids.
            val moduleBuildDirs =
                provider {
                    allprojects.associate { it.path to it.layout.buildDirectory.get().asFile }
                }
            tasks.register<TestSummaryTask>("testSummary") {
                moduleBuildDirectories.set(moduleBuildDirs)
                rootBuildDirectory.set(layout.buildDirectory)
                outputFile.set(layout.buildDirectory.file("reports/test-summary/index.html"))
            }
        }
    }
}
