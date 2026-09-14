package com.gmail.volkovskiyda.abit.core.observability

import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Crashlytics, which is the only crash reporter this project has on any platform. Collection is
 * switched off for debug builds from the Application, so a developer's stack traces never mix into
 * the numbers a release is judged on.
 */
internal class FirebaseCrashReporter(
    private val crashlytics: FirebaseCrashlytics = FirebaseCrashlytics.getInstance(),
) : CrashReporter {
    override fun log(message: String) = crashlytics.log(message)

    override fun recordException(throwable: Throwable) = crashlytics.recordException(throwable)

    override fun setUserId(id: String?) = crashlytics.setUserId(id.orEmpty())

    override fun setCustomKey(
        key: String,
        value: String,
    ) = crashlytics.setCustomKey(key, value)
}
