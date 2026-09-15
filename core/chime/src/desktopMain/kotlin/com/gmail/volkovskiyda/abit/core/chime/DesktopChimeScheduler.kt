package com.gmail.volkovskiyda.abit.core.chime

import com.gmail.volkovskiyda.abit.core.common.LocalClock
import com.gmail.volkovskiyda.abit.core.datastore.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope

/**
 * The Mac's scheduler. Nothing beyond [PollingChimeScheduler] — the lid-open case is exactly the
 * wall-clock re-read the shared loop already does, and the menu bar reads
 * [PollingChimeScheduler.minutesLeft] rather than being pushed to.
 */
class DesktopChimeScheduler(
    scope: CoroutineScope,
    clock: LocalClock,
    preferences: UserPreferencesRepository,
    bell: Bell,
    coordinator: () -> ChimeCoordinator,
) : PollingChimeScheduler(scope, clock, preferences, bell, coordinator)
