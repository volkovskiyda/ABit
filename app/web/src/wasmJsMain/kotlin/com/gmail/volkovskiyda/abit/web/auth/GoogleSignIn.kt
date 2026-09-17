package com.gmail.volkovskiyda.abit.web.auth

import com.gmail.volkovskiyda.abit.app.shared.FirebaseConfig
import kotlinx.coroutines.CompletableDeferred

/**
 * Google sign-in in the browser, through Google Identity Services.
 *
 * The shape deliberately matches the phone's, the watch's and the Mac's: this hands back an **id
 * token** and `core:auth` is what exchanges it for a Firebase user — which is also what links it to
 * the anonymous account instead of replacing it, so the schedules made before signing in survive.
 *
 * GIS rather than Firebase's own `signInWithPopup`, and that is the reason why. `signInWithPopup`
 * signs in directly, replacing whoever is signed in, which would make the browser the one platform
 * where signing in silently discards local work. The id token is the seam every other platform
 * already goes through.
 *
 * The client id is [FirebaseConfig.WEB_OAUTH_CLIENT_ID] and it is committed, like the API key beside
 * it: a browser client id ships inside every bundle that uses it and identifies the project rather
 * than authorising anything. What protects it is the authorised-JavaScript-origins list on the
 * client, which is why the origin matters and the secrecy does not.
 */
internal class GoogleSignIn {
    /**
     * False until the project has a web OAuth client — the same state the Android apps are in while
     * `google-services.json` carries no `default_web_client_id` — and false until Google's script
     * has loaded, which `index.html` requests with `async defer`. Settings says so rather than
     * offering a button that cannot work.
     */
    val available: Boolean get() = FirebaseConfig.WEB_OAUTH_CLIENT_ID.isNotBlank() && googleAccountsId() != null

    /**
     * Opens Google's account chooser and resolves with the id token it returns.
     *
     * The prompt can decline to show — third-party sign-in blocked, the user dismissed it too often,
     * an origin missing from the client — and GIS reports that through the moment listener rather
     * than by failing, so it is turned into a failed [Result] here. Without that, the button would
     * appear to do nothing at all.
     *
     * Both paths race into one [CompletableDeferred] and `complete` is idempotent, which is what
     * makes the ordering safe: a dismissal notification arrives for a *successful* sign-in too, just
     * after the credential callback has already completed it.
     */
    suspend fun requestIdToken(): Result<String> {
        val accounts = googleAccountsId() ?: return Result.failure(IllegalStateException(NOT_CONFIGURED))
        val token = CompletableDeferred<Result<String>>()

        val configuration = gsiConfiguration(FirebaseConfig.WEB_OAUTH_CLIENT_ID)
        configuration.callback = { response -> token.complete(Result.success(response.credential)) }
        accounts.initialize(configuration)
        accounts.prompt { notification ->
            if (notification.getMomentType() != DISPLAY_MOMENT) {
                token.complete(Result.failure(IllegalStateException(NOT_SHOWN)))
            }
        }
        return token.await()
    }

    private companion object {
        const val DISPLAY_MOMENT = "display"
        const val NOT_CONFIGURED = "Google sign-in is not configured in this build"
        const val NOT_SHOWN =
            "Google did not complete the sign-in. Allow third-party sign-in for this site, or sign in " +
                "on another device and this browser will catch up."
    }
}

/**
 * `google.accounts.id`, as external declarations rather than as JavaScript in a `js(…)` body.
 *
 * The distinction matters for the two callbacks: passing a Kotlin lambda across the boundary is
 * something Kotlin/Wasm supports on an *external declaration* — a method parameter, a property — and
 * writing the same thing inside a `js(…)` string would be handing the compiler a name it cannot
 * check. Everything below is therefore a declaration, and the single `js(…)` call reaches the global
 * the `<script>` tag in index.html defines.
 */
private external interface GsiAccountsId : JsAny {
    fun initialize(configuration: GsiConfiguration)

    fun prompt(listener: (GsiNotification) -> Unit)
}

private external interface GsiConfiguration : JsAny {
    var callback: (GsiCredentialResponse) -> Unit
}

private external interface GsiCredentialResponse : JsAny {
    /** The id token, as a JWT. */
    val credential: String
}

private external interface GsiNotification : JsAny {
    /** `"display"`, `"skipped"` or `"dismissed"`. */
    fun getMomentType(): String
}

/**
 * Null until Google's script has loaded, and null forever if it is blocked — which is a state this
 * app has to render rather than crash on, so the check is on `typeof` rather than on the property.
 */
private fun googleAccountsId(): GsiAccountsId? =
    js("(typeof google !== 'undefined' && google.accounts && google.accounts.id) ? google.accounts.id : null")

/**
 * `auto_select` is false deliberately: with a session already on the machine GIS will otherwise sign
 * someone straight back in after they signed out, which is the one thing this flow must not do.
 *
 * The suppression is for `clientId`, which is read inside the `js(…)` body where detekt cannot see
 * it — the same one `core:chime`'s Web Audio wrappers carry.
 */
@Suppress("UnusedParameter")
private fun gsiConfiguration(clientId: String): GsiConfiguration =
    js("({ client_id: clientId, auto_select: false, cancel_on_tap_outside: true })")
