package com.gmail.volkovskiyda.abit.core.observability.di

import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * The desktop and web builds report nothing. Crashlytics has no artifact for either target, and
 * adding a second vendor for them was decided against — so the no-op bindings from
 * [observabilityModule] stand and this module adds nothing.
 */
actual val platformObservabilityModule: Module = module { }
