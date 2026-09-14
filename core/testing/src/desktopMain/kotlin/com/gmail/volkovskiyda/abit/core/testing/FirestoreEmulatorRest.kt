package com.gmail.volkovskiyda.abit.core.testing

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.util.Base64

/**
 * Talks to the Firestore emulator over its REST API, as a specific signed-in user.
 *
 * This exists because the security rules are the one artefact in this project that fails silently
 * and expensively: a wrong rule does not break a build or a test, it exposes one user's data to
 * another. They deserve a test that actually attempts the access and asserts it is refused.
 *
 * It deliberately does **not** go through the client SDK. GitLive's JVM target reaches Firebase
 * through a port of the Firebase Android SDK whose auth never completes a call on a plain JVM (see
 * the note in the infrastructure plan), so an SDK-based rules test could not run at all. The REST
 * API is also the more honest thing to test against: it is what the rules actually guard.
 *
 * The emulator accepts an *unsigned* JWT and trusts its claims, which is what makes acting as an
 * arbitrary uid possible without an auth server. That is emulator-only behaviour by design.
 */
class FirestoreEmulatorRest(
    private val projectId: String = "abit-kmp",
) {
    private val host: String =
        requireNotNull(Emulator.firestoreHost) {
            "FIRESTORE_EMULATOR_HOST is unset — run through scripts/emulator-tests.sh"
        }

    /**
     * HTTP status of creating a one-field document named [documentId] in [collection], as [uid].
     *
     * A POST to the collection rather than a PATCH to the document, because `HttpURLConnection`
     * rejects PATCH outright ("Invalid HTTP method") and this test has no business pulling in an
     * HTTP client to work around it. Firestore's createDocument endpoint is equivalent for the
     * purpose: the rules see a write to the same path either way.
     */
    fun create(
        collection: String,
        documentId: String,
        uid: String,
        field: String,
        value: String,
    ): Int =
        request(
            method = "POST",
            path = "$collection?documentId=$documentId",
            uid = uid,
            body = """{"fields":{"$field":{"stringValue":"$value"}}}""",
        )

    /** HTTP status of reading the document at [path] as [uid]. */
    fun get(
        path: String,
        uid: String,
    ): Int = request(method = "GET", path = path, uid = uid)

    private fun request(
        method: String,
        path: String,
        uid: String,
        body: String? = null,
    ): Int {
        val url =
            URI(
                "http://$host/v1/projects/$projectId/databases/(default)/documents/$path",
            ).toURL()
        val connection =
            (url.openConnection() as HttpURLConnection).apply {
                requestMethod = method
                setRequestProperty("Authorization", "Bearer ${unsignedJwt(uid)}")
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = TIMEOUT_MILLIS
                readTimeout = TIMEOUT_MILLIS
                if (body != null) {
                    doOutput = true
                    outputStream.use { it.write(body.toByteArray()) }
                }
            }
        return try {
            connection.responseCode
        } catch (e: IOException) {
            // A refused read surfaces here on some JDKs rather than as a status code.
            connection.responseCode.takeIf { it > 0 } ?: throw e
        } finally {
            connection.disconnect()
        }
    }

    /**
     * An unsigned JWT carrying just the claims the rules read. The emulator does not verify the
     * signature, which is the whole point: no auth server is needed to act as a given user.
     */
    private fun unsignedJwt(uid: String): String {
        val header = base64Url("""{"alg":"none","typ":"JWT"}""")
        val payload =
            base64Url(
                """{"iss":"https://securetoken.google.com/$projectId","aud":"$projectId",""" +
                    """"sub":"$uid","user_id":"$uid","provider_id":"anonymous"}""",
            )
        return "$header.$payload."
    }

    private fun base64Url(json: String): String = Base64.getUrlEncoder().withoutPadding().encodeToString(json.toByteArray())

    private companion object {
        const val TIMEOUT_MILLIS = 10_000
        const val HTTP_FORBIDDEN = 403
    }
}
