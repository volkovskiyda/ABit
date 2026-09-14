package com.gmail.volkovskiyda.abit.app.shared

/**
 * The Firebase project's public identifiers, for the platforms that have no `google-services.json`
 * to read them from — the desktop and web builds, which initialise the SDK by hand.
 *
 * These are committed deliberately. Every one of them ships inside any web bundle or Android APK
 * that talks to Firebase, so they are public by construction, and Firebase's own documentation says
 * as much: an API key here identifies the project, it does not authorise anything. What actually
 * protects the data is `firestore.rules`, which lets a user read and write only their own documents.
 *
 * The browser key is additionally restricted by HTTP referrer to the Hosting domain in the Google
 * Cloud console, so a copy of it lifted from the bundle cannot be used from someone else's page.
 *
 * The Android builds do **not** read this object. They use the real `google-services.json`, which is
 * git-ignored and restored in CI from a secret — not because the values differ in kind, but because
 * that file also carries the OAuth client ids that Google sign-in resolves against.
 */
object FirebaseConfig {
    const val PROJECT_ID: String = "abit-kmp"
    const val PROJECT_NUMBER: String = "335064890942"
    const val API_KEY: String = "AIzaSyA1APeMTPpX8_vB5aK79DjDthk-93cntWk"
    const val APP_ID_WEB: String = "1:335064890942:web:a734457746b2f9f8b71b6f"
    const val AUTH_DOMAIN: String = "abit-kmp.firebaseapp.com"
    const val STORAGE_BUCKET: String = "abit-kmp.firebasestorage.app"
}
