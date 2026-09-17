package com.gmail.volkovskiyda.abit.core.auth

import com.gmail.volkovskiyda.abit.core.testing.Emulator
import com.gmail.volkovskiyda.abit.core.testing.MainDispatcherRule
import com.google.firebase.FirebasePlatform
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.initialize
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import android.content.Context as FirebaseJvmContext

/**
 * Google sign-in on the Mac, performed for real against the Auth emulator.
 *
 * This is the one path in the project that no SDK implements: `firebase-java-sdk` leaves
 * `signInWithCredential` and `linkWithCredential` as `TODO()`, so
 * [signInWithGoogleCredential] speaks Identity Toolkit itself and then hands the result to the
 * SDK through a private setter it reaches by reflection. Both halves fail the same way on a
 * version bump — silently at first, and then in front of someone who has just signed in — so both
 * are exercised here rather than reasoned about.
 *
 * The emulator accepts an *unsigned* Google id token, a plain JSON object, and trusts its claims.
 * That is emulator-only behaviour by design, and it is what makes a Google sign-in testable with
 * no Google account and no credentials.
 *
 * Does nothing when the Auth emulator is not running — this one needs no Firestore, so it asks for
 * the host it actually uses rather than for [Emulator.isRunning]. Run it with
 * `scripts/emulator-tests.sh`.
 */
class GoogleSignInEmulatorTest {
    private val mainDispatcher = MainDispatcherRule()
    private val running: Boolean get() = Emulator.authHost != null

    @BeforeTest
    fun setUp() {
        if (!running) return
        // The SDK's own calls (anonymous sign-in, delete) route through the emulator only once
        // told to; `signInWithGoogleCredential` reads FIREBASE_AUTH_EMULATOR_HOST for itself.
        mainDispatcher.setUp()
        firebase
    }

    @AfterTest
    fun tearDown() {
        if (!running) return
        mainDispatcher.tearDown()
    }

    @Test
    fun `signing in with Google keeps the anonymous account's uid`() =
        runTest {
            if (!running) return@runTest
            val repository = FirebaseAuthRepository()
            val anonymous = repository.signInAnonymously().getOrThrow()

            val signedIn = repository.signInWithGoogle(googleIdToken(subject = "keeps-uid")).getOrThrow()

            assertEquals(anonymous.id, signedIn.id, "the anonymous account was replaced instead of linked")
            assertFalse(signedIn.isAnonymous)
            assertEquals("keeps-uid@example.com", signedIn.email)
        }

    @Test
    fun `the signed-in user is the SDK's, not only this call's`() =
        runTest {
            if (!running) return@runTest
            val repository = FirebaseAuthRepository()
            repository.signInAnonymously().getOrThrow()

            val signedIn = repository.signInWithGoogle(googleIdToken(subject = "reaches-sdk")).getOrThrow()

            // Everything downstream reads the user from here: Firestore takes its bearer token from
            // it, and the tray listens to it. A sign-in the SDK does not know about syncs nothing.
            val current = assertNotNull(Firebase.auth.currentUser, "the SDK has no signed-in user")
            assertEquals(signedIn.id.value, current.uid)
            assertFalse(current.isAnonymous)
            assertNotNull(current.getIdToken(false), "the adopted user cannot produce an id token")
        }

    @Test
    fun `signing into a Google account that already exists resolves in the account's favour`() =
        runTest {
            if (!running) return@runTest
            val repository = FirebaseAuthRepository()
            val token = googleIdToken(subject = "already-exists")
            repository.signInAnonymously().getOrThrow()
            val first = repository.signInWithGoogle(token).getOrThrow()
            repository.signOut()

            val second = repository.signInAnonymously().getOrThrow()
            val resolved = repository.signInWithGoogle(token).getOrThrow()

            assertEquals(first.id, resolved.id, "the collision did not resolve in the account's favour")
            assertTrue(resolved.id != second.id, "the anonymous account swallowed an existing Google account")
        }

    private companion object {
        /**
         * One Firebase, for the whole class. `initializeFirebasePlatform` and `initialize` each
         * refuse a second call, and JUnit makes no promise about how many instances of a test class
         * it creates.
         */
        val firebase: Unit by lazy {
            FirebasePlatform.initializeFirebasePlatform(InMemoryPlatform())
            Firebase.initialize(
                // An android.content.Context on the JVM, because firebase-java-sdk is a port of the
                // Android SDK and GitLive's JVM binding casts to it — the same thing the desktop
                // app does in InitFirebase.desktop.kt.
                context = FirebaseJvmContext(),
                options =
                    FirebaseOptions(
                        applicationId = "1:0:web:emulator",
                        apiKey = "emulator-ignores-this",
                        projectId = "abit-kmp",
                    ),
            )
            val (host, port) = requireNotNull(Emulator.authHost).split(":")
            Firebase.auth.useEmulator(host, port.toInt())
        }

        /** What Google would have signed, in the form the emulator accepts instead: unsigned JSON. */
        fun googleIdToken(subject: String): String =
            """{"sub":"$subject","email":"$subject@example.com","email_verified":true,"name":"Test User"}"""
    }
}

/** Storage that lasts as long as the test JVM, so no run inherits the last one's signed-in user. */
private class InMemoryPlatform : FirebasePlatform() {
    private val values = mutableMapOf<String, String>()

    override fun store(
        key: String,
        value: String,
    ) {
        values[key] = value
    }

    override fun retrieve(key: String): String? = values[key]

    override fun clear(key: String) {
        values.remove(key)
    }

    override fun log(msg: String) = Unit
}
