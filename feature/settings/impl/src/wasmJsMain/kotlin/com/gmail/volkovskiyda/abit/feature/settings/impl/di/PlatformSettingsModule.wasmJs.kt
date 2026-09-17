package com.gmail.volkovskiyda.abit.feature.settings.impl.di

import com.gmail.volkovskiyda.abit.feature.settings.api.PermissionId
import com.gmail.volkovskiyda.abit.feature.settings.api.PermissionReader
import com.gmail.volkovskiyda.abit.feature.settings.api.PermissionState
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformSettingsModule: Module =
    module {
        single<PermissionReader> {
            PermissionReader {
                listOf(PermissionState(PermissionId.BrowserNotifications, notificationsGranted()))
            }
        }
    }

/**
 * Guarded on `typeof Notification`: the API is absent in an insecure context, and reading
 * `Notification.permission` there is a `ReferenceError` rather than a `false`.
 */
private fun notificationsGranted(): Boolean = js("typeof Notification !== 'undefined' && Notification.permission === 'granted'")
