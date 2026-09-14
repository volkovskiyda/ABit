import com.gmail.volkovskiyda.abit.buildlogic.library
import com.gmail.volkovskiyda.abit.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/**
 * A `:feature:*:impl` module. It holds the feature's presentation logic — use cases, repositories
 * and a multiplatform `ViewModel` — and deliberately no Compose: the UI is written per platform in
 * the app modules, so a feature that pulled in Compose would be claiming to own screens it does not.
 *
 * The matching `:feature:*:api` module uses [KmpLibraryConventionPlugin] instead, and carries only
 * navigation keys and model types, so features can navigate to each other without depending on each
 * other's implementation.
 */
class KmpFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("abit.kmp.library")

            dependencies {
                add("commonMainApi", project(":core:common"))
                add("commonMainApi", project(":core:model"))
                add("commonMainApi", project(":core:domain"))
                add("commonMainApi", project(":core:observability"))

                add("commonMainApi", libs.library("androidx-lifecycle-viewmodel"))
                add("commonMainApi", libs.library("koin-core-viewmodel"))
                add("commonMainApi", libs.library("androidx-navigation3-runtime"))

                add("commonTestImplementation", project(":core:testing"))
            }
        }
    }
}
