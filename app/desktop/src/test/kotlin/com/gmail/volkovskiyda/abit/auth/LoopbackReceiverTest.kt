package com.gmail.volkovskiyda.abit.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The half of desktop Google sign-in that can be tested without Google: the loopback listener the
 * browser is redirected back to.
 *
 * It is also the half worth testing. The token exchange is one HTTPS POST against Google's endpoint,
 * but this listener is where a redirect from somewhere else could hand the app a code — so the
 * `state` check below is the security-relevant assertion, not a formality.
 */
class LoopbackReceiverTest {
    @Test
    fun `hands over the code the browser was redirected with`() =
        runTest {
            LoopbackReceiver().use { receiver ->
                val code = async { receiver.awaitCode(expectedState = "the-state") }
                receiver.redirect("code=the-code&state=the-state")
                assertEquals("the-code", code.await().getOrNull())
            }
        }

    @Test
    fun `refuses a code that came back with someone else's state`() =
        runTest {
            LoopbackReceiver().use { receiver ->
                val code = async { receiver.awaitCode(expectedState = "the-state") }
                receiver.redirect("code=the-code&state=a-different-state")
                val error = code.await().exceptionOrNull()
                assertTrue(error?.message?.contains("did not match") == true, "was: $error")
            }
        }

    /** Closing the consent tab is a cancel, and the popover words it as one. */
    @Test
    fun `reports a refused consent as a cancellation`() =
        runTest {
            LoopbackReceiver().use { receiver ->
                val code = async { receiver.awaitCode(expectedState = "the-state") }
                receiver.redirect("error=access_denied&state=the-state")
                assertEquals("Sign-in cancelled", code.await().exceptionOrNull()?.message)
            }
        }

    @Test
    fun `decodes a percent-encoded code`() =
        runTest {
            LoopbackReceiver().use { receiver ->
                val code = async { receiver.awaitCode(expectedState = "the-state") }
                receiver.redirect("code=4%2F0Ab_x%3D%3D&state=the-state")
                assertEquals("4/0Ab_x==", code.await().getOrNull())
            }
        }

    /** The build has no `oauth.properties`, which is the state every fresh clone is in. */
    @Test
    fun `reports sign-in unavailable without an OAuth client`() {
        assertEquals(null, DesktopOAuthConfig.fromResources())
        assertTrue(!GoogleSignIn(config = null).available)
    }

    /** Plays the part of the browser: fetches the redirect URI the way Google's redirect would. */
    private suspend fun LoopbackReceiver.redirect(query: String) {
        val request = HttpRequest.newBuilder(URI("$redirectUri/?$query")).GET().build()
        withContext(Dispatchers.IO) {
            HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())
        }
    }
}
