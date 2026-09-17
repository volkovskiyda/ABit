package com.gmail.volkovskiyda.abit.core.chime

import com.gmail.volkovskiyda.abit.core.datastore.ChimeSound

/** The same bell a boundary rings, so what the user hears in Settings is what they will get. */
class DesktopChimePreview(
    private val bell: Bell,
) : ChimePreview {
    override suspend fun chime(sound: ChimeSound) = bell.ring(sound)

    /** A Mac has nothing to buzz, and the setting is not offered here. */
    override suspend fun vibrate() = Unit
}
