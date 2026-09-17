package com.gmail.volkovskiyda.abit.feature.settings.impl.di

import com.gmail.volkovskiyda.abit.feature.settings.api.PermissionReader
import org.koin.core.module.Module
import org.koin.dsl.module

/** The Mac asks for nothing: it chimes from its own process and posts nothing the system gates. */
actual val platformSettingsModule: Module =
    module {
        single<PermissionReader> { PermissionReader { emptyList() } }
    }
