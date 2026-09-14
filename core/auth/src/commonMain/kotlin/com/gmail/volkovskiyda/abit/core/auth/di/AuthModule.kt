package com.gmail.volkovskiyda.abit.core.auth.di

import com.gmail.volkovskiyda.abit.core.auth.FirebaseAuthRepository
import com.gmail.volkovskiyda.abit.core.auth.NoopAuthRepository
import com.gmail.volkovskiyda.abit.core.common.firebaseAvailable
import com.gmail.volkovskiyda.abit.core.domain.AuthRepository
import org.koin.dsl.module

val authModule =
    module {
        // Touching Firebase Auth without an initialised FirebaseApp throws, and a build with no
        // credentials must still run — so the no-op stands in and every call fails with a message the
        // UI can show rather than a crash.
        single<AuthRepository> {
            if (firebaseAvailable()) FirebaseAuthRepository() else NoopAuthRepository
        }
    }
