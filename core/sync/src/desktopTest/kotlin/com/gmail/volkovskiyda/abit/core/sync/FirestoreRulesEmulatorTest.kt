package com.gmail.volkovskiyda.abit.core.sync

import com.gmail.volkovskiyda.abit.core.testing.Emulator
import com.gmail.volkovskiyda.abit.core.testing.FirestoreEmulatorRest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * What `firestore.rules` actually allows, attempted rather than reviewed.
 *
 * These rules are the only thing separating two users' data, and getting them wrong fails silently:
 * nothing crashes, a stranger can simply read your schedules. So each case here performs the access
 * for real against the emulator and asserts on the status code.
 *
 * Does nothing when the emulators are not running. Run it with `scripts/emulator-tests.sh`.
 */
class FirestoreRulesEmulatorTest {
    private val alice = "alice-uid"
    private val bob = "bob-uid"

    @Test
    fun `a user may write and read their own schedule`() {
        if (!Emulator.isRunning) return
        val firestore = FirestoreEmulatorRest()
        assertEquals(
            OK,
            firestore.create("users/$alice/schedules", documentId = "own", uid = alice, field = "id", value = "own"),
        )
        assertEquals(OK, firestore.get("users/$alice/schedules/own", uid = alice))
    }

    @Test
    fun `a user may not read another user's schedule`() {
        if (!Emulator.isRunning) return
        val firestore = FirestoreEmulatorRest()
        assertEquals(
            OK,
            firestore.create("users/$alice/schedules", documentId = "private", uid = alice, field = "id", value = "p"),
        )

        val status = firestore.get("users/$alice/schedules/private", uid = bob)

        assertTrue(status == FORBIDDEN || status == NOT_FOUND, "Bob read Alice's schedule: HTTP $status")
    }

    @Test
    fun `a user may not write into another user's collection`() {
        if (!Emulator.isRunning) return
        val firestore = FirestoreEmulatorRest()

        val status =
            firestore.create("users/$alice/schedules", documentId = "injected", uid = bob, field = "id", value = "x")

        assertEquals(FORBIDDEN, status, "Bob wrote into Alice's collection")
    }

    @Test
    fun `the same denial covers the day overrides collection`() {
        if (!Emulator.isRunning) return
        val firestore = FirestoreEmulatorRest()
        assertEquals(
            OK,
            firestore.create("users/$alice/dayOverrides", documentId = "2026-09-15", uid = alice, field = "id", value = "d"),
        )

        val status = firestore.get("users/$alice/dayOverrides/2026-09-15", uid = bob)

        assertTrue(status == FORBIDDEN || status == NOT_FOUND, "Bob read Alice's day override: HTTP $status")
    }

    @Test
    fun `nothing outside the users tree is writable`() {
        if (!Emulator.isRunning) return
        val firestore = FirestoreEmulatorRest()

        // The catch-all deny is what stops a new collection inheriting access by accident.
        val status = firestore.create("scratch", documentId = "anything", uid = alice, field = "id", value = "nope")

        assertEquals(FORBIDDEN, status, "a collection outside /users accepted a write")
    }

    private companion object {
        const val OK = 200
        const val FORBIDDEN = 403
        const val NOT_FOUND = 404
    }
}
