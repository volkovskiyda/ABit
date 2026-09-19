package com.gmail.volkovskiyda.abit.wear.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialOption
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Google sign-in through Credential Manager on the **watch**. A near-copy of the phone's, and
 * deliberately so: the two apps share an application id but not a source set, and one shared
 * Android-only module for forty lines would buy less than it costs.
 *
 * The watch needs its own sign-in because it syncs through Firestore rather than through a paired
 * phone — and only a Google-linked account syncs at all. Credential Manager arrived in Wear OS 4; on
 * an older watch the call fails and [requestIdToken] reports it, which the card handles by leaving
 * the user on anonymous rather than crashing.
 *
 * It is the only supported path on Android 14+ — the old Google Sign-In SDK is retired. It hands
 * back an id token; exchanging that for a Firebase user is `core:auth`'s job, so this stays a thin
 * platform shim.
 *
 * `serverClientId` is the *web* OAuth client id from the Firebase project, not the Android one.
 * That trips everyone up once: Google issues the token for the backend that will verify it, and for
 * Firebase Auth that backend is identified by the web client.
 */
class GoogleSignIn(
    private val context: Context,
) {
    /**
     * The web OAuth client id, from the resource the Google Services plugin generates out of
     * `google-services.json` — looked up by name rather than through `R`, because the resource only
     * exists once a web OAuth client does, and referencing it directly would stop a build without
     * one from compiling at all.
     *
     * Null only on a build whose `google-services.json` predates the project's web OAuth client;
     * with the client in place the resource exists and the card offers the button.
     *
     * A name lookup is invisible to the resource shrinker, which removed the string from every
     * release APK until `res/raw/abit_keep.xml` started naming it — so every release watch fell back
     * to "Sign in on your phone". That file is what keeps this working.
     */
    val serverClientId: String? by lazy {
        val id = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (id == 0) null else context.getString(id)
    }

    /**
     * Two options, tried in order, and the second one is why this is not a copy of the phone's.
     *
     * `GetGoogleIdOption` is the saved-account sheet, and on the phone it is the whole story. On a
     * watch it answers [NoCredentialException] — "no Google account on this device" — on a watch
     * that demonstrably has one and is paired to a phone that signs in with the same build and the
     * same OAuth client. Only credential providers with a Wear integration serve a request here, and
     * the saved-account option is not one of them.
     *
     * `GetSignInWithGoogleOption` is the explicit "Sign in with Google" button flow, which is the one
     * the Wear OS authentication guide points at, and which matches what the card actually says.
     * Falling back rather than replacing keeps the better sheet wherever a provider does serve it.
     */
    suspend fun requestIdToken(serverClientId: String): Result<String> =
        runCatching {
            val savedAccount =
                GetGoogleIdOption
                    .Builder()
                    .setServerClientId(serverClientId)
                    // false, so someone signing in for the first time still sees their accounts. Filtering
                    // to previously authorised accounts shows an empty sheet to every new user.
                    .setFilterByAuthorizedAccounts(false)
                    .build()

            runCatching { credential(savedAccount) }
                .recoverCatching { error ->
                    if (error is NoCredentialException) {
                        credential(GetSignInWithGoogleOption.Builder(serverClientId).build())
                    } else {
                        throw error
                    }
                }.getOrThrow()
        }.recoverCatching { error ->
            throw IllegalStateException(error.reason(), error)
        }

    private suspend fun credential(option: CredentialOption): String {
        val response =
            CredentialManager.create(context).getCredential(
                context = context,
                request = GetCredentialRequest.Builder().addCredentialOption(option).build(),
            )
        return GoogleIdTokenCredential.createFrom(response.credential.data).idToken
    }

    /**
     * Every Credential Manager failure the UI has to word differently. `NoCredentialException` is the
     * one that is not really an error — the device has no Google account to offer — and it is also the
     * one Android lint insists is handled somewhere in the project.
     */
    private fun Throwable.reason(): String =
        when (this) {
            // Reached only when the explicit sign-in flow also declined, so the account is not simply
            // missing — saying "no account" here sent one debugging session looking at the watch's
            // account list, which had two. Credential Manager's own message names the real reason
            // (an unsupported API level, a provider with no Wear integration), so it wins over
            // anything this app could guess; the fallback is for the case where it has none.
            is NoCredentialException -> message ?: "No Google account this app can use"

            is GetCredentialCancellationException -> "Sign-in cancelled"

            is GetCredentialException -> message ?: "Sign-in failed"

            else -> message ?: "Sign-in failed"
        }
}
