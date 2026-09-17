package com.gmail.volkovskiyda.abit.core.chime

import com.gmail.volkovskiyda.abit.core.datastore.ChimeSound

/**
 * The same bell a boundary rings — and, because the preview only ever runs from the tap that
 * changed the setting, it doubles as the user gesture Web Audio requires: picking a sound is the
 * moment the suspended `AudioContext` is allowed to start.
 */
class WebChimePreview(
    private val bell: WebBell,
) : ChimePreview {
    override suspend fun chime(sound: ChimeSound) {
        bell.resumeOnGesture()
        bell.ring(sound)
    }

    /** A browser notification cannot carry a live countdown, so the tab itself is the only one. */
    override suspend fun countdown() = Unit
}
