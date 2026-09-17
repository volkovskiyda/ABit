package com.gmail.volkovskiyda.abit.core.chime

import com.gmail.volkovskiyda.abit.core.common.LocalClock
import com.gmail.volkovskiyda.abit.core.datastore.UserPreferences
import com.gmail.volkovskiyda.abit.core.datastore.UserPreferencesRepository
import com.gmail.volkovskiyda.abit.core.domain.DayOverrideRepository
import com.gmail.volkovskiyda.abit.core.domain.ScheduleRepository
import com.gmail.volkovskiyda.abit.core.domain.chimesFrom
import com.gmail.volkovskiyda.abit.core.domain.todayState
import com.gmail.volkovskiyda.abit.core.model.DayOverride
import com.gmail.volkovskiyda.abit.core.model.Schedule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/** Everything the coordinator needs to decide what the platform should be doing. */
private data class Inputs(
    val schedules: List<Schedule>,
    val overrides: Map<LocalDate, DayOverride>,
    val preferences: UserPreferences,
)

/**
 * The one place that decides what the platform should be doing right now: what to chime next, and
 * what the ongoing countdown should say.
 *
 * It recomputes on every change to the schedules, the overrides or this device's preferences, and
 * once more after each chime fires — so it never has to reason about time passing, only about the
 * plan changing and about being told the alarm went off.
 *
 * There is no per-device mute to consult: a device running the app chimes its schedule, and the
 * place to silence one is the platform's own notification settings, which every platform already
 * has and which work whether the app is running or not.
 */
class ChimeCoordinator(
    private val scheduleRepository: ScheduleRepository,
    private val dayOverrideRepository: DayOverrideRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val scheduler: ChimeScheduler,
    private val clock: LocalClock,
    private val scope: CoroutineScope,
) {
    /**
     * Called once, from the composition root. No dispatcher argument: the injected application scope
     * already carries one, and overriding it here would mean two places deciding where background
     * work runs.
     */
    fun start() {
        scope.launch {
            val today = clock.today()
            combine(
                scheduleRepository.observeSchedules(),
                dayOverrideRepository.observeFrom(today),
                preferencesRepository.preferences,
                ::Inputs,
            ).collect { apply(it) }
        }
    }

    /**
     * Re-arms after a chime has fired. The platform calls this from whatever woke it, because only
     * the platform knows that the alarm it set has actually gone off.
     */
    suspend fun onChimeFired() {
        val now = clock.now()
        apply(
            Inputs(
                schedules = scheduleRepository.observeSchedules().first(),
                overrides = dayOverrideRepository.observeFrom(now.date).first(),
                preferences = preferencesRepository.preferences.first(),
            ),
        )
    }

    private suspend fun apply(inputs: Inputs) {
        val now = clock.now()
        val next =
            chimesFrom(inputs.schedules, inputs.overrides, now, limit = 1).firstOrNull()

        if (next == null) scheduler.disarm() else scheduler.arm(next)
        scheduler.showCountdown(todayState(inputs.schedules, inputs.overrides, now))
    }
}
