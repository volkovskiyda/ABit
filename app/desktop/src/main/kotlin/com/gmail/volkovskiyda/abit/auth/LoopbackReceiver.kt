package com.gmail.volkovskiyda.abit.auth

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.CompletableDeferred
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * The one-request web server Google redirects back to.
 *
 * This is the loopback flow, which is what Google documents for a desktop app: the browser is the
 * user agent, so the app cannot read the response out of it — instead it listens on a local port,
 * puts that port in the `redirect_uri`, and Google's redirect delivers the authorization code to
 * this process. Port 0 asks the OS for a free one, because a fixed port is one second instance of
 * the app away from failing to bind.
 *
 * It answers exactly one request and then stops. Nothing off this machine can reach it: the socket
 * is bound to the loopback interface, so it is not on the network at all.
 */
internal class LoopbackReceiver : AutoCloseable {
    private val callback = CompletableDeferred<Result<Callback>>()

    private val server: HttpServer =
        HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 1).apply {
            createContext("/") { exchange ->
                val query =
                    exchange.requestURI.query
                        .orEmpty()
                        .parseQuery()
                val bytes = page(failed = query["code"] == null).toByteArray(StandardCharsets.UTF_8)
                exchange.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
                exchange.sendResponseHeaders(HTTP_OK, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
                callback.complete(query.toCallback())
            }
            start()
        }

    /** `http://127.0.0.1:<port>`, which is what the request sends as its `redirect_uri`. */
    val redirectUri: String = "http://${InetAddress.getLoopbackAddress().hostAddress}:${server.address.port}"

    /**
     * The authorization code, once the browser has come back here.
     *
     * [expectedState] is checked against what arrived, so a redirect this app did not start — a
     * stale tab, a link someone sent — cannot hand it a code.
     */
    suspend fun awaitCode(expectedState: String): Result<String> =
        callback.await().mapCatching { (code, state) ->
            check(state == expectedState) { "The sign-in response did not match this request." }
            code
        }

    override fun close() {
        server.stop(0)
    }

    private data class Callback(
        val code: String,
        val state: String?,
    )

    private companion object {
        const val HTTP_OK = 200

        fun Map<String, String>.toCallback(): Result<Callback> {
            val error = this["error"]
            val code = this["code"]
            return when {
                // `access_denied` is the user closing the tab, which is a cancel rather than a failure.
                error == "access_denied" -> Result.failure(IllegalStateException("Sign-in cancelled"))

                error != null -> Result.failure(IllegalStateException("Google refused the sign-in: $error"))

                code != null -> Result.success(Callback(code, this["state"]))

                else -> Result.failure(IllegalStateException("Google returned no authorization code."))
            }
        }

        /** What the browser tab shows once the code has been handed over. */
        fun page(failed: Boolean): String {
            val message =
                if (failed) {
                    "Sign-in did not complete. You can close this tab and try again from ABit."
                } else {
                    "You are signed in. You can close this tab and go back to ABit."
                }
            return """
                |<!doctype html><html lang="en"><head><meta charset="utf-8"><title>ABit</title>
                |<style>body{font:16px/1.5 system-ui,sans-serif;margin:4rem auto;max-width:28rem;
                |padding:0 1.5rem;color:#222}@media(prefers-color-scheme:dark){body{background:#16181c;
                |color:#e6e6e6}}</style></head><body><h1>ABit</h1><p>$message</p></body></html>
                """.trimMargin()
        }

        fun String.parseQuery(): Map<String, String> =
            split("&")
                .mapNotNull { pair ->
                    pair.split("=", limit = 2).takeIf { it.size == 2 }?.let { (key, value) ->
                        key to URLDecoder.decode(value, StandardCharsets.UTF_8)
                    }
                }.toMap()
    }
}
