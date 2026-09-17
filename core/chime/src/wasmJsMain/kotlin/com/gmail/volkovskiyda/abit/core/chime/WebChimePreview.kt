package com.gmail.volkovskiyda.abit.core.chime

import com.gmail.volkovskiyda.abit.core.datastore.ChimeSound

/** Long enough to feel deliberate; browsers that honour `navigator.vibrate` clamp anything longer. */
private const val BUZZ_MILLIS = 120

/**
 * The same bell a boundary rings — and, because the preview only ever runs from the tap that flipped
 * the switch, it doubles as the user gesture Web Audio requires: turning chiming on is the moment the
 * suspended `AudioContext` is allowed to start.
 */
class WebChimePreview(
    private val bell: WebBell,
) : ChimePreview {
    override suspend fun chime(sound: ChimeSound) {
        bell.resumeOnGesture()
        bell.ring(sound)
    }

    /** Desktop browsers ignore this; a phone on the road honours it. */
    override suspend fun vibrate() {
        vibrateOnce(BUZZ_MILLIS)
    }
}

// The parameter is read inside the `js(…)` body, which detekt cannot see.
@Suppress("UnusedParameter")
private fun vibrateOnce(millis: Int): Unit =
    js("{ if (typeof navigator !== 'undefined' && navigator.vibrate) { navigator.vibrate(millis); } }")
