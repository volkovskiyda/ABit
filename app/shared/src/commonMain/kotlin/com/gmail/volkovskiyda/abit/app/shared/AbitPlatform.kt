package com.gmail.volkovskiyda.abit.app.shared

/**
 * Which of the four apps is running, as a label for the monitoring session.
 *
 * It is a parameter of [initKoin] rather than an `expect`/`actual` because `app:shared`'s
 * `androidMain` is compiled into both the phone app and the watch app: no source set can tell those
 * two apart, but their entry points can, and they are the only place that knows.
 *
 * Kotzilla groups sessions by app and by version string and offers no platform facet of its own, so
 * [id] is prefixed onto the reported version — see `abitMonitoring`. That makes the version the
 * platform axis as well: `desktop-1.0` beside `android-1.0`, in one app, rather than four apps that
 * cannot be compared with each other.
 */
enum class AbitPlatform(
    val id: String,
) {
    Android("android"),
    Wear("wear"),
    Desktop("desktop"),
    Web("web"),
}
