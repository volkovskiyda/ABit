package com.gmail.volkovskiyda.abit.core.observability.di

import com.gmail.volkovskiyda.abit.core.common.firebaseAvailable
import com.gmail.volkovskiyda.abit.core.observability.AndroidTracer
import com.gmail.volkovskiyda.abit.core.observability.CrashReporter
import com.gmail.volkovskiyda.abit.core.observability.FirebaseCrashReporter
import com.gmail.volkovskiyda.abit.core.observability.NoopCrashReporter
import com.gmail.volkovskiyda.abit.core.observability.Tracer
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformObservabilityModule: Module =
    module {
        // Falls back to the no-op reporter in a build with no google-services.json: touching Crashlytics
        // without a FirebaseApp throws, and a keyless build must run.
        single<CrashReporter> {
            if (firebaseAvailable()) FirebaseCrashReporter() else NoopCrashReporter
        }
        single<Tracer> { if (firebaseAvailable()) AndroidTracer() else com.gmail.volkovskiyda.abit.core.observability.NoopTracer }
    }
