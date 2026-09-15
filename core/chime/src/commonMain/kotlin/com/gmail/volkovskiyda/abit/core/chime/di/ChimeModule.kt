package com.gmail.volkovskiyda.abit.core.chime.di

import com.gmail.volkovskiyda.abit.core.chime.ChimeCoordinator
import com.gmail.volkovskiyda.abit.core.common.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import org.koin.core.module.Module
import org.koin.dsl.module

val chimeModule: Module =
    module {
        single {
            ChimeCoordinator(
                scheduleRepository = get(),
                dayOverrideRepository = get(),
                preferencesRepository = get(),
                scheduler = get(),
                clock = get(),
                scope = get<CoroutineScope>(ApplicationScope),
            )
        }
    }

/** The [com.gmail.volkovskiyda.abit.core.chime.ChimeScheduler] for this platform. */
expect val platformChimeModule: Module
