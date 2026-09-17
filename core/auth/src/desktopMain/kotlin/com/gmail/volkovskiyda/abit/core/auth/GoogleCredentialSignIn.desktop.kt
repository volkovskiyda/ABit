package com.gmail.volkovskiyda.abit.core.auth

import com.gmail.volkovskiyda.abit.core.domain.AuthUser
import com.gmail.volkovskiyda.abit.core.model.UserId
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseUserImpl
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import com.google.firebase.auth.FirebaseAuth as JavaFirebaseAuth

/**
 * Google sign-in on the Mac, spoken directly to Identity Toolkit.
 *
 * `firebase-java-sdk` — the port of the Firebase Android SDK that GitLive uses on the JVM — leaves
 * `signInWithCredential` and `linkWithCredential` as `TODO()`, measured against 0.6.3, the newest
 * release there is. So the Mac does for itself what the SDK does everywhere else: one POST to
 * `accounts:signInWithIdp`, the same endpoint and the same request the SDK would have sent. Sending
 * the anonymous user's own id token along with it is what makes that call a **link** rather than a
 * replacement, which is the whole point — see [FirebaseAuthRepository].
 *
 * The response then has to become the SDK's signed-in user, or nothing else would notice it:
 * Firestore takes its bearer token from `FirebaseAuth`, the tray reads `authStateChanged`, and the
 * refresh token belongs in the store that keeps the Mac signed in across restarts. The SDK offers
 * no public way to hand it one — `updateCurrentUser` is another `TODO()` — so [adopt] reaches its
 * private setter by reflection. That is the ugly part, and it is deliberately the *only* ugly part:
 * everything either side of it is the SDK's own type and the SDK's own behaviour. It is covered by
 * `GoogleSignInEmulatorTest`, which fails loudly if a version bump moves it.
 */
internal actual suspend fun FirebaseAuth.signInWithGoogleCredential(
    idToken: String,
    link: FirebaseUser?,
): AuthUser {
    val linkTo =
        link?.let {
            // A stale token makes Identity Toolkit refuse the link, and the caller would then throw
            // the anonymous account away for a reason that was never the account's fault.
            checkNotNull(it.getIdToken(false)) { "The anonymous account has no id token to link." }
        }
    return signInWithIdp(googleIdToken = idToken, linkTo = linkTo).adopt()
}

/**
 * POSTs the Google id token to Identity Toolkit, as a link when [linkTo] is a Firebase id token.
 *
 * A failure here is ordinary: the commonest one is `FEDERATED_USER_ID_ALREADY_LINKED`, which is
 * Firebase saying that Google account is already a user of its own. The caller reads that as the
 * collision it is and signs into the existing account instead.
 */
private suspend fun signInWithIdp(
    googleIdToken: String,
    linkTo: String?,
): JsonObject {
    val body =
        buildJsonObject {
            // Identity Toolkit takes the provider's token form-encoded inside the JSON, in the
            // shape the OAuth redirect it is standing in for would have had.
            put("postBody", "id_token=${googleIdToken.urlEncoded()}&providerId=$GOOGLE_PROVIDER")
            put("requestUri", REQUEST_URI)
            put("returnSecureToken", true)
            if (linkTo != null) put("idToken", linkTo)
        }
    val request =
        HttpRequest
            .newBuilder(URI(endpoint()))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build()
    val response =
        withContext(Dispatchers.IO) {
            HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())
        }
    check(response.statusCode() == HTTP_OK) { "Firebase refused the Google sign-in: ${response.body().reason()}" }
    return Json.parseToJsonElement(response.body()).jsonObject
}

/**
 * Makes [this] response the SDK's signed-in user.
 *
 * `FirebaseUserImpl` reads exactly the fields Identity Toolkit returns, so the SDK builds the same
 * object from the same JSON when it signs in anonymously — this only has to put it where the SDK
 * puts it. Its setter is what persists the refresh token and notifies every auth state listener, so
 * going around it and writing the field would sign the user in for this process only.
 */
private fun JsonObject.adopt(): AuthUser {
    val app = FirebaseApp.getInstance()
    val user = FirebaseUserImpl(app = app, data = this)
    val setUser =
        runCatching {
            JavaFirebaseAuth::class
                .java
                .getDeclaredMethod("setUser", FirebaseUserImpl::class.java)
                .apply { isAccessible = true }
        }.getOrElse { throw IllegalStateException(SETTER_GONE, it) }
    setUser.invoke(JavaFirebaseAuth.getInstance(app), user)
    return AuthUser(
        id = UserId(user.uid),
        isAnonymous = false,
        displayName = user.displayName,
        email = user.email,
    )
}

/**
 * Production, unless `FIREBASE_AUTH_EMULATOR_HOST` says otherwise — the variable the Firebase CLI
 * exports into whatever `emulators:exec` starts, and the switch every Firebase SDK reads. It is
 * what lets this path be tested against a real Identity Toolkit implementation without an account.
 */
private fun endpoint(): String {
    val root =
        System
            .getenv("FIREBASE_AUTH_EMULATOR_HOST")
            ?.let { "http://$it/$IDENTITY_TOOLKIT" }
            ?: "https://$IDENTITY_TOOLKIT"
    return "$root/v1/accounts:signInWithIdp?key=${FirebaseApp.getInstance().options.apiKey}"
}

/** Firebase's own word for what went wrong, and never the body, which echoes the request back. */
private fun String.reason(): String =
    runCatching {
        Json
            .parseToJsonElement(this)
            .jsonObject["error"]
            ?.jsonObject
            ?.get("message")
            ?.jsonPrimitive
            ?.contentOrNull
    }.getOrNull() ?: "the request was rejected"

private fun String.urlEncoded(): String = URLEncoder.encode(this, StandardCharsets.UTF_8)

private const val IDENTITY_TOOLKIT = "identitytoolkit.googleapis.com"
private const val GOOGLE_PROVIDER = "google.com"
private const val REQUEST_URI = "http://localhost"
private const val HTTP_OK = 200
private const val SETTER_GONE =
    "firebase-java-sdk no longer has FirebaseAuth.setUser, so the Mac cannot complete a Google " +
        "sign-in. Check whether the version in use implements signInWithCredential, and delete " +
        "GoogleCredentialSignIn.desktop.kt if it does."
