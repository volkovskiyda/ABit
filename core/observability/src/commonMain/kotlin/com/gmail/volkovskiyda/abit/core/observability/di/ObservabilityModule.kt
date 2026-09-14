package com.gmail.volkovskiyda.abit.core.observability.di

import com.gmail.volkovskiyda.abit.core.observability.CrashReporter
import com.gmail.volkovskiyda.abit.core.observability.NoopCrashReporter
import com.gmail.volkovskiyda.abit.core.observability.NoopTracer
import com.gmail.volkovskiyda.abit.core.observability.Tracer
import org.koin.dsl.module

/**
 * The no-op bindings every platform starts from. Android overrides both with the Crashlytics and
 * Perfetto implementations through a platform module, which is why these are plain `single`s and
 * not `singleOf` — a later `module` in the list wins.
 */
val observabilityModule =
    module {
        single<CrashReporter> { NoopCrashReporter }
        single<Tracer> { NoopTracer }
    }
