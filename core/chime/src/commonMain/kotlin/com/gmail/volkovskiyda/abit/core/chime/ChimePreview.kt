package com.gmail.volkovskiyda.abit.core.chime

import com.gmail.volkovskiyda.abit.core.datastore.ChimeSound

/**
 * One boundary's worth of sound, played on demand.
 *
 * Settings changes a chime setting and then shows what it did: the sound is otherwise a claim the
 * user cannot check until the next boundary, by which point they have stopped wondering.
 *
 * Deliberately not [Bell]: there is no `Bell` on Android at all — the system owns the boundary sound
 * there — and a preview has to make a noise on every platform the switch appears on.
 */
interface ChimePreview {
    /** The boundary sound, once. Silent when that is what the user has chosen. */
    suspend fun chime(sound: ChimeSound)

    /** A sample of the ongoing countdown, where a platform has one. A no-op where it does not. */
    suspend fun countdown()
}
