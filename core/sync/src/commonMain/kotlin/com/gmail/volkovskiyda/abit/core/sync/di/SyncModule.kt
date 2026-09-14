package com.gmail.volkovskiyda.abit.core.sync.di

import com.gmail.volkovskiyda.abit.core.domain.SyncStatusRepository
import com.gmail.volkovskiyda.abit.core.sync.UnavailableSyncStatusRepository
import org.koin.dsl.module

val syncModule = module {
    single<SyncStatusRepository> { UnavailableSyncStatusRepository }
}
