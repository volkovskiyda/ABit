package com.gmail.volkovskiyda.abit.core.datastore.di

import androidx.datastore.core.DataStore
import com.gmail.volkovskiyda.abit.core.datastore.LocalStorageDataStore
import com.gmail.volkovskiyda.abit.core.datastore.USER_PREFERENCES_FILE_NAME
import com.gmail.volkovskiyda.abit.core.datastore.UserPreferences
import com.gmail.volkovskiyda.abit.core.datastore.UserPreferencesSerializer
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformDatastoreModule: Module =
    module {
        single<DataStore<UserPreferences>> {
            LocalStorageDataStore(key = USER_PREFERENCES_FILE_NAME, serializer = UserPreferencesSerializer)
        }
    }
