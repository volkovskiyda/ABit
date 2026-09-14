package com.gmail.volkovskiyda.abit.core.data.di

import com.gmail.volkovskiyda.abit.core.data.OfflineFirstPomodoroSessionRepository
import com.gmail.volkovskiyda.abit.core.domain.PomodoroSessionRepository
import org.koin.dsl.module

val dataModule = module {
    single<PomodoroSessionRepository> { OfflineFirstPomodoroSessionRepository() }
}
