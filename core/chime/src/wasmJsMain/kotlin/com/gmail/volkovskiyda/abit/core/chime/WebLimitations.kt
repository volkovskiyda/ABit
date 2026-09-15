package com.gmail.volkovskiyda.abit.core.chime

/**
 * The sentence the settings screen and the web app's Today screen render, so the two cannot invent
 * different copy for the same limitation.
 *
 * It is a limitation worth stating rather than hiding: a browser throttles a background tab's timers
 * to roughly one wake a minute and stops a discarded tab entirely, so the web app is the one surface
 * that genuinely cannot promise a chime.
 */
const val WEB_CHIME_LIMITATION: String =
    "Chimes only sound while this tab is open. Your phone, watch and Mac are not affected."
