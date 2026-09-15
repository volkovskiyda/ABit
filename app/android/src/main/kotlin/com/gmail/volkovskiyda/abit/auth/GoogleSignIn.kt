package com.gmail.volkovskiyda.abit.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Google sign-in through Credential Manager, which is the only supported path on Android 14+ — the
 * old Google Sign-In SDK is retired. It hands back an id token; exchanging that for a Firebase user
 * is `core:auth`'s job, so this stays a thin platform shim.
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
     * It is null on this project today: creating that client needs the OAuth consent screen to be
     * configured in the Google Cloud console, which is an interactive step. Until then Google
     * sign-in reports itself unavailable and anonymous sign-in carries the app.
     */
    val serverClientId: String? by lazy {
        val id = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (id == 0) null else context.getString(id)
    }

    suspend fun requestIdToken(serverClientId: String): Result<String> =
        runCatching {
            val option =
                GetGoogleIdOption
                    .Builder()
                    .setServerClientId(serverClientId)
                    // false, so someone signing in for the first time still sees their accounts. Filtering
                    // to previously authorised accounts shows an empty sheet to every new user.
                    .setFilterByAuthorizedAccounts(false)
                    .build()

            val response =
                CredentialManager.create(context).getCredential(
                    context = context,
                    request = GetCredentialRequest.Builder().addCredentialOption(option).build(),
                )
            GoogleIdTokenCredential.createFrom(response.credential.data).idToken
        }.recoverCatching { error ->
            throw IllegalStateException(error.reason(), error)
        }

    /**
     * Every Credential Manager failure the UI has to word differently. `NoCredentialException` is the
     * one that is not really an error — the device has no Google account to offer — and it is also the
     * one Android lint insists is handled somewhere in the project.
     */
    private fun Throwable.reason(): String =
        when (this) {
            is NoCredentialException -> "No Google account on this device"
            is GetCredentialCancellationException -> "Sign-in cancelled"
            is GetCredentialException -> message ?: "Sign-in failed"
            else -> message ?: "Sign-in failed"
        }
}
