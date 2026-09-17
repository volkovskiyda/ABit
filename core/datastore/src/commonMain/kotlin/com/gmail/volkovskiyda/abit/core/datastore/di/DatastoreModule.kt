package com.gmail.volkovskiyda.abit.core.datastore.di

import com.gmail.volkovskiyda.abit.core.common.di.ApplicationScope
import com.gmail.volkovskiyda.abit.core.datastore.UserPreferencesRepository
import org.koin.core.module.Module
import org.koin.dsl.module

val datastoreModule: Module =
    module {
        // The process scope, not a screen's: the repository reads the file once and holds the
        // result for everything that asks afterwards.
        single { UserPreferencesRepository(get(), get(ApplicationScope), get()) }
    }

/**
 * The `DataStore<UserPreferences>` itself, which no two platforms build the same way — and on wasm
 * is not built by DataStore at all: version 1.2.1's `DataStoreFactory` actual for that target is
 * `TODO()`, so the browser gets a hand-rolled store over `localStorage` that reuses this project's
 * serializer so the stored format still matches.
 */
expect val platformDatastoreModule: Module
