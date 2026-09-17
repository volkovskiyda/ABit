package com.gmail.volkovskiyda.abit.core.common

/**
 * The running build's version name — `<tag>.<commit count>`, matching the release asset it came
 * from, or `1.0` for a build compiled locally.
 *
 * It exists because no ABit app updates itself: the phone APK, the watch APK and the DMG are all
 * installed by hand from a GitHub release, so "which one am I running?" is a question the app has to
 * be able to answer. The value is bound in `app:shared`, which is the only module that knows it;
 * `docs/INSTALL.md` is what the answer is compared against.
 */
data class AppVersion(
    val name: String,
)
