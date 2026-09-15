@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    // The convention plugins. An included build rather than buildSrc, so editing one does not
    // invalidate every task in the main build.
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // PREFER_SETTINGS rather than FAIL_ON_PROJECT_REPOS: the Kotlin/Wasm plugin registers its own
    // Node distribution repository on the root project and cannot be told not to. Preferring
    // settings keeps resolution owned here — the ivy repositories below are what actually serve
    // Node, Yarn and Binaryen — while letting that registration exist instead of failing the build.
    repositoriesMode = RepositoriesMode.PREFER_SETTINGS
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()

        // The Kotlin/Wasm browser toolchain downloads its own Node and Yarn. Declared here and
        // scoped to exactly the modules they serve, rather than left to the Kotlin plugin.
        ivy("https://nodejs.org/dist") {
            name = "Node Distributions"
            patternLayout { artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]") }
            metadataSources { artifact() }
            content { includeModule("org.nodejs", "node") }
        }
        ivy("https://github.com/yarnpkg/yarn/releases/download") {
            name = "Yarn Distributions"
            patternLayout { artifact("v[revision]/[artifact](-v[revision]).[ext]") }
            metadataSources { artifact() }
            content { includeModule("com.yarnpkg", "yarn") }
        }
        ivy("https://github.com/WebAssembly/binaryen/releases/download") {
            name = "Binaryen Distributions"
            patternLayout { artifact("version_[revision]/[artifact]-version_[revision]-[classifier].[ext]") }
            metadataSources { artifact() }
            content { includeModule("com.github.webassembly", "binaryen") }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "ABit"

include(":core:common")
include(":core:model")
include(":core:domain")
include(":core:database")
include(":core:datastore")
include(":core:designsystem")
include(":core:auth")
include(":core:sync")
include(":core:chime")
include(":core:observability")
include(":core:data")
include(":core:testing")

// The api/impl split, from the first feature onwards: `:api` holds a feature's navigation key and
// the types a host needs, so features can navigate to each other without depending on each other's
// implementation. `:impl` holds the presentation logic and is depended on only by `:app:shared`.
include(":feature:pomodoro:api")
include(":feature:pomodoro:impl")

include(":app:shared")
include(":app:android")
include(":app:wear")
include(":app:desktop")
include(":app:web")

include(":baselineprofile")

check(JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_17)) {
    """
    ABit requires JDK 17+ but it is currently using JDK ${JavaVersion.current()}.
    Java Home: [${System.getProperty("java.home")}]
    """.trimIndent()
}
