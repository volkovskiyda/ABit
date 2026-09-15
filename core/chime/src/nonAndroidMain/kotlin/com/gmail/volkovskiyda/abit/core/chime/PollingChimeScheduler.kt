package com.gmail.volkovskiyda.abit.core.chime

import com.gmail.volkovskiyda.abit.core.common.LocalClock
import com.gmail.volkovskiyda.abit.core.datastore.UserPreferencesRepository
import com.gmail.volkovskiyda.abit.core.domain.Chime
import com.gmail.volkovskiyda.abit.core.domain.TodayState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.toInstant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * A boundary that passed while the machine was asleep or the tab was hidden is fired on the way back
 * — but only if it is fresher than this. Chiming for a break that ended an hour ago is noise, not a
 * reminder.
 */
internal val STALE_AFTER: Duration = 5.minutes

/** How long the loop is allowed to sleep before it re-reads the wall clock. */
private val POLL_INTERVAL: Duration = 1.minutes

/**
 * The desktop and the browser have no OS alarm to delegate to, so each holds a live coroutine — and
 * each has a host that interferes with one: a laptop sleeps, a background tab is throttled to about
 * a wake a minute. The loop below survives both by never trusting a single long `delay`: it wakes at
 * least once a minute, re-reads the **wall clock**, and decides again.
 *
 * The countdown is published as [minutesLeft] rather than drawn: the macOS menu bar (item 15) and
 * the web page read it, and neither belongs in `core:chime`.
 */
open class PollingChimeScheduler(
    private val scope: CoroutineScope,
    private val clock: LocalClock,
    private val preferences: UserPreferencesRepository,
    private val bell: Bell,
    private val coordinator: () -> ChimeCoordinator,
) : ChimeScheduler {
    private val armedState = MutableStateFlow<Chime?>(null)
    private val minutesLeftState = MutableStateFlow<Int?>(null)
    private val stageState = MutableStateFlow<TodayState?>(null)

    /** What the menu bar and the page render. Null means nothing is running. */
    val minutesLeft: StateFlow<Int?> = minutesLeftState.asStateFlow()

    /** The whole state, for a surface that wants more than the minutes. */
    val todayState: StateFlow<TodayState?> = stageState.asStateFlow()

    val armed: StateFlow<Chime?> = armedState.asStateFlow()

    private var job: Job? = null

    override suspend fun arm(chime: Chime) {
        stopWaiting()
        armedState.value = chime
        job = scope.launch { waitAndRing(chime) }
    }

    override suspend fun disarm() {
        stopWaiting()
        armedState.value = null
        minutesLeftState.value = null
    }

    /**
     * Ends the wait for the currently armed boundary — **unless it is the one asking**.
     *
     * [waitAndRing] finishes by telling the coordinator a chime fired, and the coordinator answers
     * by arming the next boundary or disarming; both land back here, on the very coroutine that is
     * still running [waitAndRing]. `cancelAndJoin()` on that job therefore cancels the caller, and
     * `join()` throws [kotlinx.coroutines.CancellationException] before [arm] reaches its relaunch.
     * Desktop and web rang their first boundary and then went silent for the rest of the session —
     * the countdown stopped with them — until an unrelated schedule or preference change re-entered
     * the coordinator from its own collector.
     *
     * Clearing the field is enough for that case: the job is about to complete on its own, and the
     * caller wants what comes *after* it, not its cancellation.
     */
    private suspend fun stopWaiting() {
        val waiting = job ?: return
        job = null
        if (waiting == currentCoroutineContext()[Job]) return
        waiting.cancelAndJoin()
    }

    override suspend fun showCountdown(state: TodayState) {
        stageState.value = state
        minutesLeftState.value = (state as? TodayState.Running)?.remaining?.inWholeMinutes?.toInt()
    }

    /**
     * Called when the host has a reason to think time jumped — a lid opening, a tab becoming visible.
     * The loop would notice within a minute anyway; this makes it immediate.
     */
    suspend fun recheck() {
        val chime = armedState.value ?: return
        arm(chime)
    }

    private suspend fun waitAndRing(chime: Chime) {
        val target = chime.at.toInstant(clock.zone())
        while (currentCoroutineContext().isActive) {
            val remaining = target - clock.instant()
            if (remaining <= Duration.ZERO) break
            delay(minOf(remaining, POLL_INTERVAL))
        }

        val lateness = clock.instant() - target
        if (lateness <= STALE_AFTER) {
            val sound = preferences.preferences.first().chimeSound
            bell.ring(sound)
            onRang(chime)
        }
        armedState.value = null
        coordinator().onChimeFired()
    }

    /** A hook for the platforms that do more than make a noise — the browser posts a notification. */
    protected open suspend fun onRang(chime: Chime) = Unit
}
