package com.gmail.volkovskiyda.abit.core.sync.di

import com.gmail.volkovskiyda.abit.core.sync.FirestoreScheduleRemoteSource
import com.gmail.volkovskiyda.abit.core.sync.ScheduleRemoteSource
import org.koin.dsl.module

/**
 * The remote sources only. `SyncStatusRepository` is bound in `core:data`, where `SyncEngine` lives:
 * it is the thing that owns the sync state, and it needs the local DAO as well as this.
 */
val syncModule =
    module {
        single<ScheduleRemoteSource> { FirestoreScheduleRemoteSource() }
    }
