package com.gmail.volkovskiyda.abit.core.datastore.di

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import com.gmail.volkovskiyda.abit.core.common.AppDirs
import com.gmail.volkovskiyda.abit.core.common.di.ApplicationScope
import com.gmail.volkovskiyda.abit.core.datastore.USER_PREFERENCES_FILE_NAME
import com.gmail.volkovskiyda.abit.core.datastore.UserPreferences
import com.gmail.volkovskiyda.abit.core.datastore.UserPreferencesSerializer
import kotlinx.coroutines.CoroutineScope
import okio.FileSystem
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformDatastoreModule: Module =
    module {
        single<DataStore<UserPreferences>> {
            DataStoreFactory.create(
                storage =
                    OkioStorage(
                        fileSystem = FileSystem.SYSTEM,
                        serializer = UserPreferencesSerializer,
                        producePath = { AppDirs.path(USER_PREFERENCES_FILE_NAME).toPath() },
                    ),
                scope = get<CoroutineScope>(ApplicationScope),
            )
        }
    }
