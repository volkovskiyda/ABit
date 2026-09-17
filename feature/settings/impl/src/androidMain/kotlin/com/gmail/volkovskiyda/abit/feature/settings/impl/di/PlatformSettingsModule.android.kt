package com.gmail.volkovskiyda.abit.feature.settings.impl.di

import com.gmail.volkovskiyda.abit.core.chime.ChimePermissions
import com.gmail.volkovskiyda.abit.feature.settings.api.PermissionId
import com.gmail.volkovskiyda.abit.feature.settings.api.PermissionReader
import com.gmail.volkovskiyda.abit.feature.settings.api.PermissionState
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * The phone and the watch share this: both hold the same two permissions, and both read them with
 * [ChimePermissions] rather than with a `checkSelfPermission` of their own.
 */
actual val platformSettingsModule: Module =
    module {
        single<PermissionReader> {
            val permissions: ChimePermissions = get()
            PermissionReader {
                listOf(
                    PermissionState(PermissionId.Notifications, permissions.canPostNotifications()),
                    PermissionState(PermissionId.ExactAlarms, permissions.canScheduleExactAlarms()),
                )
            }
        }
    }
