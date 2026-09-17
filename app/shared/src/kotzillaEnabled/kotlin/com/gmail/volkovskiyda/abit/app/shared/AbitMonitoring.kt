package com.gmail.volkovskiyda.abit.app.shared

import io.kotzilla.generated.monitoring
import org.koin.core.KoinApplication

/**
 * Opens the Kotzilla session, reporting the running platform as part of the version.
 *
 * This is the keyed half of the seam and the only place allowed to name a Kotzilla type: the
 * plugin brings the SDK runtime with it, so `KotzillaCore` — the receiver `setVersion` is called
 * on — exists here and nowhere else. [initKoin] calls this instead of the generated `monitoring()`
 * directly, which is what keeps `KotzillaCore` out of `commonMain`; see the disabled twin.
 *
 * The version the plugin bakes in is the bare number, the same on all four platforms, and the SDK
 * never looks at the Android `versionName` on the Koin path (only the manual `setupAndConnect` does
 * that). So the tag is composed here rather than read back: [platform] prefixed onto [versionName],
 * which the Android apps pass from `BuildConfig.VERSION_NAME` so that AGP's `versionNameSuffix`
 * stays the one place `-debug` is spelled. Phone debug reports `android-1.0-debug` and phone release
 * `android-1.0`; the Mac and the browser have no build types and report `desktop-1.0` and `web-1.0`.
 *
 * `onConfig` runs last inside the generated `monitoring()` — after it has applied the API key and
 * the baked-in version — so this `setVersion` is the one that survives.
 */
internal fun KoinApplication.abitMonitoring(
    platform: AbitPlatform,
    versionName: String,
) {
    monitoring { setVersion("${platform.id}-$versionName") }
}
