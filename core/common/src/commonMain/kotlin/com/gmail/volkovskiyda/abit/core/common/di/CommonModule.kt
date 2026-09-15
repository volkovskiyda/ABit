package com.gmail.volkovskiyda.abit.core.common.di

import com.gmail.volkovskiyda.abit.core.common.DefaultDispatcherProvider
import com.gmail.volkovskiyda.abit.core.common.DispatcherProvider
import com.gmail.volkovskiyda.abit.core.common.Logger
import com.gmail.volkovskiyda.abit.core.common.SystemTimeProvider
import com.gmail.volkovskiyda.abit.core.common.SystemTimeZoneProvider
import com.gmail.volkovskiyda.abit.core.common.TimeProvider
import com.gmail.volkovskiyda.abit.core.common.TimeZoneProvider
import com.gmail.volkovskiyda.abit.core.common.platformLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** Qualifier for the process-lifetime scope, so it is never confused with a screen's scope. */
val ApplicationScope = named("applicationScope")

val commonModule =
    module {
        single<DispatcherProvider> { DefaultDispatcherProvider() }
        single<TimeProvider> { SystemTimeProvider() }
        single<TimeZoneProvider> { SystemTimeZoneProvider() }
        single<Logger> { platformLogger() }
        // SupervisorJob so one failed background job does not cancel every other one for the process.
        single(ApplicationScope) {
            CoroutineScope(SupervisorJob() + get<DispatcherProvider>().default)
        }
    }
