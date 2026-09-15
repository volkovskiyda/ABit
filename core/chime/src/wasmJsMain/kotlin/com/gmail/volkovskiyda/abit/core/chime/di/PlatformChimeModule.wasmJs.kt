package com.gmail.volkovskiyda.abit.core.chime.di

import com.gmail.volkovskiyda.abit.core.chime.Bell
import com.gmail.volkovskiyda.abit.core.chime.ChimeScheduler
import com.gmail.volkovskiyda.abit.core.chime.WebBell
import com.gmail.volkovskiyda.abit.core.chime.WebChimeScheduler
import com.gmail.volkovskiyda.abit.core.common.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformChimeModule: Module =
    module {
        single<Bell> { WebBell() }
        single<ChimeScheduler> {
            WebChimeScheduler(
                scope = get<CoroutineScope>(ApplicationScope),
                clock = get(),
                preferences = get(),
                bell = get(),
                // Lazy: the coordinator holds the scheduler, so an eager get here would be a cycle.
                coordinator = { get() },
            )
        }
    }
