package com.gmail.volkovskiyda.abit.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.awt.Desktop
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import kotlin.time.Duration.Companion.minutes

/**
 * How long the loopback listener waits for the browser to come back.
 *
 * Long enough for a password, a second factor and a moment's hesitation; short enough that an
 * abandoned attempt gives its port and its thread back the same afternoon.
 */
private val SIGN_IN_TIMEOUT = 5.minutes

/**
 * Google sign-in on the Mac, through the loopback flow Google documents for a desktop app.
 *
 * The shape deliberately matches the phone's and the watch's: this hands back an **id token** and
 * `core:auth` is what exchanges it for a Firebase user — which is also what links it to the
 * anonymous account instead of replacing it. Only the way the token is obtained differs, because
 * macOS has no Credential Manager.
 *
 * What happens when [requestIdToken] is called:
 *
 * 1. A one-request server starts on a loopback port ([LoopbackReceiver]).
 * 2. The system browser opens Google's consent page, carrying a PKCE challenge and a random `state`.
 * 3. Google redirects back to that port with an authorization code.
 * 4. The code plus the PKCE *verifier* are exchanged for an id token, over HTTPS, from this process.
 *
 * PKCE is what makes step 4 safe without a secret worth protecting: the code is useless to anyone
 * who did not generate the verifier, which never leaves this process until the exchange. That is why
 * a Desktop-app client is the right kind here — see [DesktopOAuthConfig].
 */

internal class GoogleSignIn(
    private val config: DesktopOAuthConfig? = DesktopOAuthConfig.fromResources(),
    private val browser: (URI) -> Unit = ::openInBrowser,
) {
    /**
     * False on a checkout with no `oauth.properties`, which the UI shows instead of a button that
     * cannot work. The same rule the Android apps follow when there is no web OAuth client.
     */
    val available: Boolean get() = config != null

    suspend fun requestIdToken(): Result<String> {
        val client = config ?: return Result.failure(IllegalStateException(NOT_CONFIGURED))
        return runCatching {
            LoopbackReceiver().use { receiver ->
                val verifier = randomUrlSafe()
                val state = randomUrlSafe()
                browser(authorizationUri(client, receiver.redirectUri, verifier, state))
                // Bounded, because nothing else ends this wait. Closing the consent tab sends no
                // redirect, so an abandoned sign-in suspended forever — and `use` never ran, which
                // left the bound port and its dispatch thread behind. One per abandoned attempt, in
                // a process that stays open all day.
                val code =
                    withTimeoutOrNull(SIGN_IN_TIMEOUT) { receiver.awaitCode(state) }
                        ?.getOrThrow()
                        ?: error("The sign-in was not completed.")
                exchange(client, receiver.redirectUri, code, verifier)
            }
        }
    }

    private fun authorizationUri(
        client: DesktopOAuthConfig,
        redirectUri: String,
        verifier: String,
        state: String,
    ): URI =
        URI(
            AUTH_ENDPOINT + "?" +
                mapOf(
                    "client_id" to client.clientId,
                    "redirect_uri" to redirectUri,
                    "response_type" to "code",
                    // `openid` is what makes the response carry an id token at all; the other two
                    // are what put a name and an address on the Firebase user.
                    "scope" to "openid email profile",
                    "code_challenge" to verifier.pkceChallenge(),
                    "code_challenge_method" to "S256",
                    "state" to state,
                ).toQuery(),
        )

    private suspend fun exchange(
        client: DesktopOAuthConfig,
        redirectUri: String,
        code: String,
        verifier: String,
    ): String {
        val form =
            mapOf(
                "client_id" to client.clientId,
                "client_secret" to client.clientSecret,
                "code" to code,
                "code_verifier" to verifier,
                "grant_type" to "authorization_code",
                "redirect_uri" to redirectUri,
            ).toQuery()
        val request =
            HttpRequest
                .newBuilder(URI(TOKEN_ENDPOINT))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build()
        val response =
            withContext(Dispatchers.IO) {
                HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())
            }
        check(response.statusCode() == HTTP_OK) {
            // Never the body: a failed exchange echoes the request back, client secret included.
            "Google rejected the sign-in (HTTP ${response.statusCode()})."
        }
        return Json
            .parseToJsonElement(response.body())
            .jsonObject["id_token"]
            ?.jsonPrimitive
            ?.content
            ?: error("Google returned no id token.")
    }

    private companion object {
        const val AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth"
        const val TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token"
        const val HTTP_OK = 200
        const val RANDOM_BYTES = 32
        const val NOT_CONFIGURED = "Google sign-in is not configured in this build"

        val urlEncoder: Base64.Encoder = Base64.getUrlEncoder().withoutPadding()

        fun randomUrlSafe(): String = urlEncoder.encodeToString(ByteArray(RANDOM_BYTES).also(SecureRandom()::nextBytes))

        fun String.pkceChallenge(): String =
            urlEncoder.encodeToString(
                MessageDigest.getInstance("SHA-256").digest(toByteArray(StandardCharsets.US_ASCII)),
            )

        fun Map<String, String>.toQuery(): String =
            entries.joinToString("&") { (key, value) ->
                "$key=${URLEncoder.encode(value, StandardCharsets.UTF_8)}"
            }

        fun openInBrowser(uri: URI) {
            val desktop = Desktop.getDesktop().takeIf { Desktop.isDesktopSupported() }
            check(desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
                "No browser to open — sign in on another device and this Mac will sync."
            }
            desktop.browse(uri)
        }
    }
}
