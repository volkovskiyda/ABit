package com.gmail.volkovskiyda.abit.core.datastore.di

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import com.gmail.volkovskiyda.abit.core.common.di.ApplicationScope
import com.gmail.volkovskiyda.abit.core.datastore.USER_PREFERENCES_FILE_NAME
import com.gmail.volkovskiyda.abit.core.datastore.UserPreferences
import com.gmail.volkovskiyda.abit.core.datastore.UserPreferencesSerializer
import kotlinx.coroutines.CoroutineScope
import okio.FileSystem
import okio.Path.Companion.toPath
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformDatastoreModule: Module =
    module {
        single<DataStore<UserPreferences>> {
            val path = androidContext().filesDir.resolve("datastore/$USER_PREFERENCES_FILE_NAME")
            DataStoreFactory.create(
                storage =
                    OkioStorage(
                        fileSystem = FileSystem.SYSTEM,
                        serializer = UserPreferencesSerializer,
                        producePath = { path.absolutePath.toPath() },
                    ),
                scope = get<CoroutineScope>(ApplicationScope),
            )
        }
    }
