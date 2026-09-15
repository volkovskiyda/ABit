package com.gmail.volkovskiyda.abit.core.chime.di

import com.gmail.volkovskiyda.abit.core.chime.AndroidChimeScheduler
import com.gmail.volkovskiyda.abit.core.chime.ChimeNotifications
import com.gmail.volkovskiyda.abit.core.chime.ChimePermissions
import com.gmail.volkovskiyda.abit.core.chime.ChimeScheduler
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformChimeModule: Module =
    module {
        single { ChimePermissions(androidContext()) }
        single { ChimeNotifications(context = androidContext(), permissions = get()) }
        single<ChimeScheduler> {
            AndroidChimeScheduler(
                context = androidContext(),
                notifications = get(),
                permissions = get(),
                preferences = get(),
                timeZoneProvider = get(),
            )
        }
    }
