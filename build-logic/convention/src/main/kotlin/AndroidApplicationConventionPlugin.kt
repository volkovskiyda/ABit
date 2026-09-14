import com.android.build.api.dsl.ApplicationExtension
import com.gmail.volkovskiyda.abit.buildlogic.AbitVersioning
import com.gmail.volkovskiyda.abit.buildlogic.configureAbitLint
import com.gmail.volkovskiyda.abit.buildlogic.configureConnectedTestGuard
import com.gmail.volkovskiyda.abit.buildlogic.libs
import com.gmail.volkovskiyda.abit.buildlogic.version
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/** The phone app and the Wear OS app. Both ship the same application id and the same signing. */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            pluginManager.apply("abit.versioning")

            val versioning = extensions.getByType<AbitVersioning>()

            extensions.configure<ApplicationExtension> {
                configureApplication(this@with, versioning)
            }

            // AGP 9 ships built-in Kotlin support, so there is no separate kotlin-android plugin to
            // apply and the Kotlin extension it registers is what carries the JVM target.
            extensions.configure<KotlinAndroidProjectExtension> {
                compilerOptions {
                    jvmTarget.set(JvmTarget.fromTarget(libs.version("jvmTarget")))
                }
            }

            configureConnectedTestGuard()
        }
    }
}

private fun ApplicationExtension.configureApplication(project: Project, versioning: AbitVersioning) {
    compileSdk = project.libs.version("androidCompileSdk").toInt()

    defaultConfig {
        minSdk = project.libs.version("androidMinSdk").toInt()
        targetSdk = project.libs.version("androidTargetSdk").toInt()
        versionCode = versioning.versionCode
        versionName = versioning.versionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // A project-local debug keystore, committed with the standard debug credentials, instead of
        // the per-machine ~/.android/debug.keystore AGP would generate. It gives this laptop, any
        // contributor and CI the same debug SHA-1, which is what the Firebase Android API key
        // restriction is pinned to. The release config is created in the signing item, and only
        // when keystore.properties exists.
        getByName("debug") {
            storeFile = project.rootProject.file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            // Install debug and release side by side.
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            // R8 through the AGP 9 DSL; `isMinifyEnabled` is the legacy spelling, and setting both
            // is what confuses the baseline-profile plugin later.
            optimization {
                enable = true
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        // Off by default since AGP 8. The apps read BuildConfig.DEBUG to keep Koin's resolution
        // logging, Crashlytics collection and Performance instrumentation out of debug builds.
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint { configureAbitLint(this) }
}
