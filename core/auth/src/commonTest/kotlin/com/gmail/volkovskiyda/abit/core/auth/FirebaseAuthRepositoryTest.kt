package com.gmail.volkovskiyda.abit.core.auth

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Koin builds the repository on whichever thread first asks for it, and on Android that is the main
 * thread. Touching `Firebase.auth` initialises Firebase Auth and reads the persisted user from disk —
 * the cost Kotzilla reported — so none of that may happen until something collects or signs in, and
 * then not on the caller's dispatcher. No real `FirebaseAuth` exists here; the provider throws, and
 * counting its calls is the whole test.
 */
class FirebaseAuthRepositoryTest {
    @Test
    fun `building the repository and reading its flow does not touch Firebase Auth`() {
        var calls = 0
        val repository =
            FirebaseAuthRepository(
                authProvider = {
                    calls++
                    error("touched")
                },
            )

        repository.currentUser

        assertEquals(0, calls)
    }

    @Test
    fun `the first touch happens on the io dispatcher, not the collector's`() =
        runTest {
            var calls = 0
            val repository =
                FirebaseAuthRepository(
                    authProvider = {
                        calls++
                        error("no Firebase in a unit test")
                    },
                    ioDispatcher = StandardTestDispatcher(testScheduler),
                )

            // An unconfined collector runs straight up to the dispatcher hop, the way a ViewModel on
            // Main.immediate does; without the hop, the provider would already have been called.
            val collected = async(UnconfinedTestDispatcher(testScheduler)) { runCatching { repository.currentUser.first() } }
            assertEquals(0, calls, "the collector's own dispatcher touched Firebase Auth")

            advanceUntilIdle()

            assertEquals(1, calls)
            assertTrue(collected.await().isFailure)
        }
}
