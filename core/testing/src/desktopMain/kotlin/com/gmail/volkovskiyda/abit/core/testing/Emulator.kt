package com.gmail.volkovskiyda.abit.core.testing

/**
 * Whether the Firebase Emulator Suite is running, and where.
 *
 * `emulators:exec` exports `FIREBASE_AUTH_EMULATOR_HOST` and `FIRESTORE_EMULATOR_HOST` into the
 * process it starts, so `scripts/emulator-tests.sh` turns the emulator tests on and a bare
 * `./gradlew desktopTest` leaves them doing nothing. That is deliberate: a plain unit-test run on a
 * laptop or a pull-request runner must not require anyone to have installed the emulators.
 *
 * There is no `FirebaseApp` here, and that is a limitation rather than a choice. GitLive reaches the
 * JVM through `firebase-java-sdk`, a port of the Firebase *Android* SDK, and on a plain JVM its auth
 * calls never complete — `signInAnonymously()` hangs with no request ever reaching the emulator
 * (measured 2026-09-14 with GitLive 3.0.0-alpha02 and firebase-java-sdk 0.6.3). So the emulator
 * tests drive Firestore over its REST API instead; see [FirestoreEmulatorRest], which is also the
 * more honest way to test security rules. Re-try the SDK path on a GitLive release.
 */
object Emulator {
    val authHost: String? get() = System.getenv("FIREBASE_AUTH_EMULATOR_HOST")
    val firestoreHost: String? get() = System.getenv("FIRESTORE_EMULATOR_HOST")

    val isRunning: Boolean get() = authHost != null && firestoreHost != null
}
