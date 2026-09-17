package com.gmail.volkovskiyda.abit.core.auth

import com.gmail.volkovskiyda.abit.core.domain.AuthUser
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.GoogleAuthProvider

/**
 * Turns a Google id token into a Firebase user, attaching it to [link] when one is given.
 *
 * Per platform for one reason. The JVM has no Firebase SDK of its own, so GitLive reaches it
 * through `firebase-java-sdk`, a port of the Android SDK — and that port leaves both
 * `FirebaseAuth.signInWithCredential` and `FirebaseUser.linkWithCredential` as `TODO()`. On the Mac
 * they therefore throw `NotImplementedError` ("An operation is not implemented") *after* Google has
 * already signed the user in, which is the worst possible moment: the consent screen succeeds and
 * the app reports a failure. Android and the browser have a real SDK and simply call it.
 *
 * Only the exchange is platform-specific. **When** to link — and what to do when the Google account
 * already exists — stays in [FirebaseAuthRepository], so that rule is written once.
 */
internal expect suspend fun FirebaseAuth.signInWithGoogleCredential(
    idToken: String,
    link: FirebaseUser?,
): AuthUser

/**
 * The SDK path, for the platforms whose SDK implements it. It lives in `commonMain` rather than in
 * each of their source sets because it is the same four lines on both, and the desktop actual is
 * the only one that differs.
 */
internal suspend fun FirebaseAuth.sdkSignInWithGoogleCredential(
    idToken: String,
    link: FirebaseUser?,
): AuthUser {
    val credential = GoogleAuthProvider.credential(idToken = idToken, accessToken = null)
    val result = link?.linkWithCredential(credential) ?: signInWithCredential(credential)
    return result.user.requireUser()
}
