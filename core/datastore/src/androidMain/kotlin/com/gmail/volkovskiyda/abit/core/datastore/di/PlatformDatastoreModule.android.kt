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
            val context = androidContext()
            DataStoreFactory.create(
                storage =
                    OkioStorage(
                        fileSystem = FileSystem.SYSTEM,
                        serializer = UserPreferencesSerializer,
                        // Resolved on the first read, on the application scope — not here. Koin builds
                        // this on main (the activity's splash and the chime engine both ask for it at
                        // launch), and `filesDir` is disk I/O. The desktop binding does the same.
                        producePath = {
                            context.filesDir
                                .resolve("datastore/$USER_PREFERENCES_FILE_NAME")
                                .absolutePath
                                .toPath()
                        },
                    ),
                scope = get<CoroutineScope>(ApplicationScope),
            )
        }
    }
