package com.gmail.volkovskiyda.abit.core.auth.di

import com.gmail.volkovskiyda.abit.core.auth.FirebaseAuthRepository
import com.gmail.volkovskiyda.abit.core.auth.NoopAuthRepository
import com.gmail.volkovskiyda.abit.core.common.DispatcherProvider
import com.gmail.volkovskiyda.abit.core.common.firebaseAvailable
import com.gmail.volkovskiyda.abit.core.domain.AuthRepository
import org.koin.dsl.module

val authModule =
    module {
        // Touching Firebase Auth without an initialised FirebaseApp throws, and a build with no
        // credentials must still run — so the no-op stands in and every call fails with a message the
        // UI can show rather than a crash.
        // The IO dispatcher is where the real one first touches Firebase Auth, which is disk work that
        // must not land on whichever thread asked Koin for it.
        single<AuthRepository> {
            if (firebaseAvailable()) {
                FirebaseAuthRepository(ioDispatcher = get<DispatcherProvider>().io)
            } else {
                NoopAuthRepository
            }
        }
    }
