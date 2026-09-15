package com.gmail.volkovskiyda.abit.core.chime.di

import com.gmail.volkovskiyda.abit.core.chime.ChimeScheduler
import com.gmail.volkovskiyda.abit.core.chime.NoOpChimeScheduler
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformChimeModule: Module =
    module {
        single<ChimeScheduler> { NoOpChimeScheduler() }
    }
