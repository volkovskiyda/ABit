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
 * That restriction is exactly why the Mac cannot share it — see [API_KEY_DESKTOP].
 *
 * The Android builds do **not** read this object. They use the real `google-services.json`, which is
 * git-ignored and restored in CI from a secret — not because the values differ in kind, but because
 * that file also carries the OAuth client ids that Google sign-in resolves against.
 */
object FirebaseConfig {
    const val PROJECT_ID: String = "abit-kmp"
    const val PROJECT_NUMBER: String = "335064890942"
    const val API_KEY_WEB: String = "AIzaSyA1APeMTPpX8_vB5aK79DjDthk-93cntWk"

    /**
     * The Mac's own key, because it cannot use the browser one.
     *
     * A referrer restriction is enforced against the `Referer` header, and a JVM sends none: every
     * call from the desktop app made with [API_KEY_WEB] comes back
     * `403 Requests from referer <empty> are blocked` — sign-in, token refresh and Firestore alike.
     * Nor is there another kind of restriction an installed app can satisfy: Google offers
     * referrer, IP address, Android signing certificate and iOS bundle id, and a Mac tray app fits
     * none of them.
     *
     * So this key is restricted by *API* instead — `identitytoolkit`, `securetoken` and `firestore`,
     * the three the Mac actually calls, and nothing else. A copy lifted from the DMG can therefore
     * do exactly what the app can do, which is reach a signed-in user's own documents and no one
     * else's, because `firestore.rules` is what decides that. Widening this list is a decision, not
     * a formality.
     */
    const val API_KEY_DESKTOP: String = "AIzaSyAbonJLaOBmi1xUFZPthZZ0BwXijSIqH6E"
    const val APP_ID_WEB: String = "1:335064890942:web:a734457746b2f9f8b71b6f"
    const val AUTH_DOMAIN: String = "abit-kmp.firebaseapp.com"
    const val STORAGE_BUCKET: String = "abit-kmp.firebasestorage.app"

    /**
     * The browser OAuth client the web app's Google sign-in asks Google for a token with.
     *
     * The same **Web application** client `google-services.json` carries as its `client_type` 3
     * entry: the one Credential Manager passes as `serverClientId` on the phone and the watch, and
     * the one Firebase Auth's Google provider is configured with. One client across those three is
     * what makes their id tokens interchangeable. The Mac is the exception, deliberately — an
     * installed app cannot keep a confidential secret, so it has a Desktop client of its own.
     *
     * Committed, like the API key above, and for the same reason: a browser client id ships inside
     * every bundle that uses it. What protects it is the authorised-JavaScript-origins list on the
     * client itself, not secrecy — `https://abit-kmp.web.app`, `https://abit-kmp.firebaseapp.com`,
     * and `http://localhost:8080` so the development server can sign in too.
     */
    const val WEB_OAUTH_CLIENT_ID: String = "335064890942-pga64lpiec6db47boum0narm6gm8pok4.apps.googleusercontent.com"
}
