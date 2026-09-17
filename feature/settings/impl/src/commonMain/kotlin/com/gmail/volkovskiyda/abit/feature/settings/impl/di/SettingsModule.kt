package com.gmail.volkovskiyda.abit.feature.settings.impl.di

import com.gmail.volkovskiyda.abit.feature.settings.impl.SettingsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val settingsModule =
    module {
        viewModelOf(::SettingsViewModel)
    }

/**
 * The `PermissionReader` this platform answers with. Which permissions are real depends on the
 * platform — the phone and the watch have two, the browser has one and the desktop has none — and
 * each one's answer is a local call cheap enough to make while the screen is being built.
 */
expect val platformSettingsModule: Module
