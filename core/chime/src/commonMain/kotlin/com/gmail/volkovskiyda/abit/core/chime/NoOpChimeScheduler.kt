package com.gmail.volkovskiyda.abit.core.chime

import com.gmail.volkovskiyda.abit.core.domain.Chime
import com.gmail.volkovskiyda.abit.core.domain.TodayState

/**
 * A scheduler that schedules nothing. It exists so the Koin graph resolves on every platform from the
 * moment this module lands, before items 08 and 09 replace it with the real Android, Wear, desktop
 * and web implementations — and so a platform that gains a target later fails loudly at the binding
 * rather than silently going quiet.
 */
class NoOpChimeScheduler : ChimeScheduler {
    override suspend fun arm(chime: Chime) = Unit

    override suspend fun disarm() = Unit

    override suspend fun showCountdown(state: TodayState) = Unit
}
