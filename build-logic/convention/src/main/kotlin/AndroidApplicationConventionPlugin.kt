import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import com.google.firebase.perf.plugin.FirebasePerfExtension
import com.google.gms.googleservices.GoogleServicesPlugin.GoogleServicesPluginConfig
import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import com.gmail.volkovskiyda.abit.buildlogic.AbitVersioning
import com.gmail.volkovskiyda.abit.buildlogic.configureAbitLint
import com.gmail.volkovskiyda.abit.buildlogic.configureBenchmarkVariants
import com.gmail.volkovskiyda.abit.buildlogic.configureConnectedTestGuard
import com.gmail.volkovskiyda.abit.buildlogic.configureManagedDevices
import com.gmail.volkovskiyda.abit.buildlogic.configureWearManagedDevices
import com.gmail.volkovskiyda.abit.buildlogic.libs
import com.gmail.volkovskiyda.abit.buildlogic.loadEnv
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
            configureFirebase()

            // The baseline-profile plugin creates its build types after this block runs, so the
            // fix-ups have to wait for finalizeDsl. Only the app that applies that plugin gets any.
            pluginManager.withPlugin("androidx.baselineprofile") {
                extensions.configure<ApplicationAndroidComponentsExtension> {
                    finalizeDsl { android -> configureBenchmarkVariants(android) }
                }
            }
        }
    }
}

/**
 * Firebase, configured once for both Android apps and only when they actually apply the plugins —
 * `withPlugin` rather than an unconditional block, so a module that has no Firebase (the baseline
 * profile module, say) is unaffected.
 *
 * The keyless-build rule applies here too, and this is the line that implements it: the Google
 * Services plugin's default is to fail the build outright when `google-services.json` is missing,
 * which every fresh clone and every fork's pull request is. WARN downgrades that to a message. The
 * app then starts with no `FirebaseApp` — `firebaseAvailable()` in core:common reports false and the
 * UI says sync is unavailable — rather than crashing.
 */
private fun Project.configureFirebase() {
    pluginManager.withPlugin("com.google.gms.google-services") {
        extensions.configure<GoogleServicesPluginConfig> {
            missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN
        }
    }

    pluginManager.withPlugin("com.google.firebase.crashlytics") {
        extensions.configure<ApplicationExtension> {
            buildTypes {
                debug {
                    // Debug is not minified, so there is no mapping worth uploading — the task would
                    // only cost build time and demand credentials on every assembleDebug, CI's too.
                    configure<CrashlyticsExtension> { mappingFileUploadEnabled = false }
                }
                release {
                    // Ship the R8 mapping so release stack traces arrive deobfuscated.
                    configure<CrashlyticsExtension> { mappingFileUploadEnabled = true }
                }
            }
        }
    }

    pluginManager.withPlugin("com.google.firebase.firebase-perf") {
        extensions.configure<ApplicationExtension> {
            buildTypes {
                debug {
                    // Debug never reports performance data (the Application gates collection to
                    // release), so the plugin's bytecode weaving would only slow every debug build.
                    configure<FirebasePerfExtension> { setInstrumentationEnabled(false) }
                }
            }
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

    val keystore = loadEnv(project.rootProject.file("keystore.properties"))

    signingConfigs {
        // A project-local debug keystore, committed with the standard debug credentials, instead of
        // the per-machine ~/.android/debug.keystore AGP would generate. It gives this laptop, any
        // contributor and CI the same debug SHA-1, which is what the Firebase Android API key
        // restriction is pinned to.
        getByName("debug") {
            storeFile = project.rootProject.file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        // Created only when keystore.properties supplies a keystore. Without it assembleRelease
        // still configures and builds, producing an *unsigned* APK — which is what a fresh clone
        // and a fork's pull request want, and is the same rule the Kotzilla and Firebase config
        // files follow.
        if (keystore.containsKey("KEYSTORE_FILE")) {
            create("release") {
                storeFile = project.rootProject.file(keystore.getValue("KEYSTORE_FILE"))
                storePassword = keystore.getValue("KEYSTORE_PASSWORD")
                keyAlias = keystore.getValue("KEY_ALIAS")
                // PKCS12, keytool's default store type: the key password *is* the store password.
                keyPassword = keystore.getValue("KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            // Install debug and release side by side.
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            // findByName, not getByName: null on a checkout with no keystore.properties, which
            // leaves the APK unsigned rather than failing configuration. minSdk 30 means AGP signs
            // with v2+ automatically, so no per-scheme flags are needed.
            signingConfig = signingConfigs.findByName("release")
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

    // The Wear app needs a round Wear image; every other Android app gets the phone devices.
    if (project.path == ":app:wear") configureWearManagedDevices() else configureManagedDevices()
}
