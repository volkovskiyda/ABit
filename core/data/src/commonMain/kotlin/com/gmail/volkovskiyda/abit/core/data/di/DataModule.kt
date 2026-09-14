package com.gmail.volkovskiyda.abit.core.data.di

import com.gmail.volkovskiyda.abit.core.common.di.ApplicationScope
import com.gmail.volkovskiyda.abit.core.data.OfflineFirstPomodoroSessionRepository
import com.gmail.volkovskiyda.abit.core.data.SyncEngine
import com.gmail.volkovskiyda.abit.core.data.deviceId
import com.gmail.volkovskiyda.abit.core.domain.PomodoroSessionRepository
import com.gmail.volkovskiyda.abit.core.domain.SyncStatusRepository
import kotlinx.coroutines.CoroutineScope
import org.koin.dsl.module

val dataModule =
    module {
        single<PomodoroSessionRepository> { OfflineFirstPomodoroSessionRepository(dao = get()) }

        single {
            SyncEngine(
                dao = get(),
                remote = get(),
                authRepository = get(),
                timeProvider = get(),
                scope = get<CoroutineScope>(ApplicationScope),
                deviceId = deviceId(),
            )
        }
        // The engine is what reports sync state, so it is the SyncStatusRepository the UI observes.
        single<SyncStatusRepository> { get<SyncEngine>() }
    }
