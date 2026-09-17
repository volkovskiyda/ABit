package com.gmail.volkovskiyda.abit.app.shared

import org.koin.core.KoinApplication

/**
 * Stands in for the keyed [abitMonitoring], and is compiled only when `app/shared/kotzilla.json` is
 * absent — see the `kotzilla { }` block and the source-set line that picks between the two in
 * `app/shared/build.gradle.kts`.
 *
 * Deliberately references no Kotzilla type. The plugin generates nothing and adds no SDK runtime
 * when it is off, so there is no `KotzillaCore` to name and no `monitoring()` to call; only Koin's
 * own [KoinApplication], which the app always has. Keeping the call site in [initKoin]
 * unconditional is the point: the composition root reads the same either way, and a fresh clone, a
 * fork's pull request and CI's keyless jobs all still compile.
 *
 * The two halves must stay signature-identical, and no build compiles both, so only running both
 * proves it — CI's keyless jobs cover this side on every push. The parameters are accepted and
 * ignored rather than omitted, so that a call passing something new fails on the keyed side loudly
 * rather than being silently dropped here.
 */
@Suppress("UnusedParameter")
internal fun KoinApplication.abitMonitoring(
    platform: AbitPlatform,
    versionName: String,
): Unit = Unit
