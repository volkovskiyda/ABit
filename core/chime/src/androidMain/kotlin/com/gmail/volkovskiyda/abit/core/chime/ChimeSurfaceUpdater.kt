package com.gmail.volkovskiyda.abit.core.chime

/**
 * Something outside the app's own process that shows the next chime and has to be told when it
 * changes. On Wear that is the tile; on the phone there is nothing of the kind, so the default is a
 * no-op and the watch app overrides the binding.
 *
 * Without this the tile would only refresh on its own `freshnessInterval` — up to twenty minutes
 * after a schedule was edited on the phone.
 */
fun interface ChimeSurfaceUpdater {
    fun onArmedChimeChanged()
}

/** The phone's answer: nothing outside the app renders a chime there. */
class NoChimeSurfaces : ChimeSurfaceUpdater {
    override fun onArmedChimeChanged() = Unit
}
