package com.gmail.volkovskiyda.abit.core.chime.di

import com.gmail.volkovskiyda.abit.core.chime.Bell
import com.gmail.volkovskiyda.abit.core.chime.ChimeScheduler
import com.gmail.volkovskiyda.abit.core.chime.DesktopBell
import com.gmail.volkovskiyda.abit.core.chime.DesktopChimeScheduler
import com.gmail.volkovskiyda.abit.core.common.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformChimeModule: Module =
    module {
        single<Bell> { DesktopBell() }
        single<ChimeScheduler> {
            DesktopChimeScheduler(
                scope = get<CoroutineScope>(ApplicationScope),
                clock = get(),
                preferences = get(),
                bell = get(),
                // Lazy on purpose: the coordinator holds the scheduler, so resolving it eagerly here
                // would be a cycle. By the time a chime fires the graph is long since built.
                coordinator = { get() },
            )
        }
    }
