package com.gmail.volkovskiyda.abit.core.chime

import com.gmail.volkovskiyda.abit.core.common.LocalClock
import com.gmail.volkovskiyda.abit.core.datastore.UserPreferencesRepository
import com.gmail.volkovskiyda.abit.core.domain.Chime
import com.gmail.volkovskiyda.abit.core.domain.ChimeKind
import kotlinx.browser.document
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The browser's scheduler. Two things beyond the shared loop:
 *
 * 1. A `visibilitychange` listener that re-checks the moment the tab comes forward. The loop would
 *    notice within a minute anyway, but a throttled tab's minute can be a long one, and the user is
 *    looking at the page right now.
 * 2. A browser notification beside the sound, **only when permission is already granted** — asking
 *    for it is the settings row's job, and a permission prompt triggered by a chime is exactly the
 *    kind of thing that gets an app blocked.
 */
class WebChimeScheduler(
    private val scope: CoroutineScope,
    clock: LocalClock,
    preferences: UserPreferencesRepository,
    bell: Bell,
    coordinator: () -> ChimeCoordinator,
) : PollingChimeScheduler(scope, clock, preferences, bell, coordinator) {
    init {
        document.addEventListener("visibilitychange") {
            if (!documentHidden()) scope.launch { recheck() }
        }
    }

    override suspend fun onRang(chime: Chime) {
        if (!notificationsGranted()) return
        val title =
            when (chime.kind) {
                ChimeKind.FocusStart -> "Focus"
                ChimeKind.BreakStart -> "Break"
                ChimeKind.DayEnd -> "Done for today"
            }
        postNotification(title, chime.scheduleName)
    }
}

private fun documentHidden(): Boolean = js("document.hidden")

private fun notificationsGranted(): Boolean = js("typeof Notification !== 'undefined' && Notification.permission === 'granted'")

// Both parameters are read inside the `js(…)` body, which detekt cannot see.
@Suppress("UnusedParameter")
private fun postNotification(
    title: String,
    body: String,
): Unit = js("{ new Notification(title, { body: body }); }")
