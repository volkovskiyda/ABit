package com.gmail.volkovskiyda.abit.buildlogic

import org.gradle.api.Project

/**
 * Versioning is a CI concern: nothing in this repository is edited per release.
 *
 * Both delivery workflows pass `-PbuildNumber=$(git rev-list --count HEAD)` — one monotonic
 * versionCode shared by App Distribution builds and tagged releases, so neither can ever install
 * "over" the other backwards. (`github.run_number` would not do: it counts per workflow.) They also
 * pass `-PbaseVersion`, the latest tag without its "v", so every published build reports
 * `<tag>.<versionCode>`: v1.3 at commit 348 ships as `1.3.348` with code `348`. Local and IDE
 * builds pass neither and stay at `1` / the fallback base version.
 *
 * Registered as an extension rather than duplicated per module because four modules need it and one
 * of them (`app:desktop`) is a plain JVM project with no Android DSL to hang it on.
 */
open class AbitVersioning(
    /** The latest release tag without its leading "v", or `1.0` when nothing passes one. */
    val baseVersion: String,
    /** The commit count, or `null` for a local build. */
    val buildNumber: Int?,
) {
    val versionCode: Int get() = buildNumber ?: 1

    val versionName: String get() = buildNumber?.let { "$baseVersion.$it" } ?: baseVersion

    /**
     * The same string, but always three dot-separated parts with a non-zero major — `jpackage`
     * rejects anything else for a DMG, and a local build would otherwise hand it a bare "1.0".
     */
    val packageVersion: String get() = "$baseVersion.${buildNumber ?: 1}"
}

internal fun Project.readAbitVersioning(): AbitVersioning = AbitVersioning(
    baseVersion = (findProperty("baseVersion") as String?) ?: "1.0",
    buildNumber = (findProperty("buildNumber") as String?)?.toIntOrNull(),
)
