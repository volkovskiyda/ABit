import com.gmail.volkovskiyda.abit.buildlogic.AbitVersioning
import com.gmail.volkovskiyda.abit.buildlogic.readAbitVersioning
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Registers the `abitVersioning` extension — `versionCode`, `versionName` and `packageVersion`
 * derived from `-PbuildNumber` and `-PbaseVersion`. See [AbitVersioning] for the formula and why
 * nothing in this repository is edited per release.
 *
 * Applied automatically by [AndroidApplicationConventionPlugin], and by hand in `app:desktop`,
 * which is a plain JVM module with no Android DSL to hang the numbers on:
 *
 * ```kotlin
 * plugins { id("abit.versioning") }
 * packageVersion = the<AbitVersioning>().packageVersion
 * ```
 */
class VersioningConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val versioning = readAbitVersioning()
            extensions.add(AbitVersioning::class.java, "abitVersioning", versioning)
        }
    }
}
