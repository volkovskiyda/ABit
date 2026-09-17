package com.gmail.volkovskiyda.abit.core.chime.di

import com.gmail.volkovskiyda.abit.core.chime.AndroidChimePreview
import com.gmail.volkovskiyda.abit.core.chime.AndroidChimeScheduler
import com.gmail.volkovskiyda.abit.core.chime.ChimeNotifications
import com.gmail.volkovskiyda.abit.core.chime.ChimePermissions
import com.gmail.volkovskiyda.abit.core.chime.ChimePreview
import com.gmail.volkovskiyda.abit.core.chime.ChimeScheduler
import com.gmail.volkovskiyda.abit.core.chime.ChimeSurfaceUpdater
import com.gmail.volkovskiyda.abit.core.chime.NoChimeSurfaces
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformChimeModule: Module =
    module {
        single { ChimePermissions(androidContext()) }
        single<ChimePreview> { AndroidChimePreview(androidContext()) }
        // The watch overrides this with its tile updater; the phone has nothing to update.
        single<ChimeSurfaceUpdater> { NoChimeSurfaces() }
        single { ChimeNotifications(context = androidContext(), permissions = get()) }
        single<ChimeScheduler> {
            AndroidChimeScheduler(
                context = androidContext(),
                notifications = get(),
                permissions = get(),
                preferences = get(),
                timeZoneProvider = get(),
                surfaces = get(),
            )
        }
    }
