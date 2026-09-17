plugins {
    `kotlin-dsl`
    // ktlint walks a project's own source sets, and an included build is not a subproject of the
    // main one — the root build's `allprojects { }` never reaches here. Applied with a literal id
    // and version because build-logic's settings has no plugin marker resolution of its own.
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
}

group = "com.gmail.volkovskiyda.abit.buildlogic"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.jvmToolchain.get().toInt()))
    }
}

dependencies {
    // compileOnly throughout: these plugins reach the convention plugins' *runtime* classpath from
    // the consuming build, which is why the root build.gradle.kts declares each of them with
    // `apply false`. Putting them on `implementation` here would load a second copy into every
    // subproject's classloader.
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.composeCompiler.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.room.gradlePlugin)
    compileOnly(libs.googleServices.gradlePlugin)
    compileOnly(libs.firebaseCrashlytics.gradlePlugin)
    compileOnly(libs.firebasePerf.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("abitKmpLibrary") {
            id = "abit.kmp.library"
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("abitKmpFeature") {
            id = "abit.kmp.feature"
            implementationClass = "KmpFeatureConventionPlugin"
        }
        register("abitAndroidApplication") {
            id = "abit.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("abitAndroidTest") {
            id = "abit.android.test"
            implementationClass = "AndroidTestConventionPlugin"
        }
        register("abitVersioning") {
            id = "abit.versioning"
            implementationClass = "VersioningConventionPlugin"
        }
        register("abitTestSummary") {
            id = "abit.test-summary"
            implementationClass = "TestSummaryConventionPlugin"
        }
    }
}
