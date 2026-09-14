package com.gmail.volkovskiyda.abit.core.auth.di

import com.gmail.volkovskiyda.abit.core.auth.NoopAuthRepository
import com.gmail.volkovskiyda.abit.core.domain.AuthRepository
import org.koin.dsl.module

/** Replaced by the Firebase-backed binding once credentials are present (plan item 09). */
val authModule =
    module {
        single<AuthRepository> { NoopAuthRepository }
    }
