package com.gmail.volkovskiyda.abit.feature.settings.impl.di

import com.gmail.volkovskiyda.abit.feature.settings.impl.SettingsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val settingsModule =
    module {
        viewModelOf(::SettingsViewModel)
    }
