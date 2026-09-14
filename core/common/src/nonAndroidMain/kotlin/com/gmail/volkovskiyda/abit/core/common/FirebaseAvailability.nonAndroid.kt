package com.gmail.volkovskiyda.abit.core.common

/**
 * Whether `initFirebase()` actually succeeded, which on these platforms is the only way to know.
 *
 * Android can ask the SDK (`FirebaseApp.getApps`), because the Google Services plugin initialises it
 * from generated resources before any of this code runs. The desktop and web builds initialise
 * Firebase by hand, so the answer is simply whether that call worked — and it does fail in the real
 * world: a JVM unit test has no Firebase platform installed, and a browser can be offline.
 */
private var initialised = false

actual fun firebaseAvailable(): Boolean = initialised

/** Called by `initFirebase()` with its own result, and by nothing else. */
fun markFirebaseInitialised(succeeded: Boolean) {
    initialised = succeeded
}
