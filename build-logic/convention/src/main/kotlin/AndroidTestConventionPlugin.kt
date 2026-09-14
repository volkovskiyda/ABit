import com.android.build.api.dsl.TestExtension
import com.gmail.volkovskiyda.abit.buildlogic.libs
import com.gmail.volkovskiyda.abit.buildlogic.version
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/**
 * A `com.android.test` module — one that instruments another app rather than shipping itself. Used
 * by `:baselineprofile`, which holds both the profile generator and the startup macrobenchmark.
 */
class AndroidTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.test")

            extensions.configure<TestExtension> {
                compileSdk = libs.version("androidCompileSdk").toInt()

                defaultConfig {
                    minSdk = libs.version("androidMinSdk").toInt()
                    targetSdk = libs.version("androidTargetSdk").toInt()
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }

                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
            }

            extensions.configure<KotlinAndroidProjectExtension> {
                compilerOptions {
                    jvmTarget.set(JvmTarget.fromTarget(libs.version("jvmTarget")))
                }
            }
        }
    }
}
