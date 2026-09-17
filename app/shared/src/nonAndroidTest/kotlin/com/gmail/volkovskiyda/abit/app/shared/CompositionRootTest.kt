package com.gmail.volkovskiyda.abit.app.shared

import com.gmail.volkovskiyda.abit.core.chime.ChimeCoordinator
import com.gmail.volkovskiyda.abit.core.chime.ChimeScheduler
import com.gmail.volkovskiyda.abit.core.common.DispatcherProvider
import com.gmail.volkovskiyda.abit.core.database.dao.ScheduleDao
import com.gmail.volkovskiyda.abit.core.datastore.UserPreferencesRepository
import com.gmail.volkovskiyda.abit.core.domain.DayOverrideRepository
import com.gmail.volkovskiyda.abit.core.domain.ScheduleRepository
import com.gmail.volkovskiyda.abit.core.domain.SyncStatusRepository
import com.gmail.volkovskiyda.abit.core.observability.CrashReporter
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Starts the real composition root and resolves what a host actually asks for — the strongest check
 * short of launching an app, because it builds every singleton rather than only inspecting the
 * definitions the way [KoinGraphTest] does.
 *
 * In `nonAndroidTest` rather than `commonTest`: the Android bindings need a `Context` that an
 * Android *host* unit test has no way to supply, and Room's bundled SQLite needs a JNI library that
 * such a test cannot load either. Both are real on a device, which is what the instrumented tests
 * cover; here the desktop JVM stands in.
 */
class CompositionRootTest {
    @AfterTest
    fun tearDown() = stopKoin()

    @Test
    fun `the composition root starts and resolves`() {
        val koin = initKoin(AbitPlatform.Desktop).koin

        assertNotNull(koin.get<DispatcherProvider>())
        assertNotNull(koin.get<ScheduleDao>())
        assertNotNull(koin.get<ScheduleRepository>())
        assertNotNull(koin.get<DayOverrideRepository>())
        assertNotNull(koin.get<UserPreferencesRepository>())
        assertNotNull(koin.get<SyncStatusRepository>())
        assertNotNull(koin.get<CrashReporter>())
        // The chime engine is the product: if these two do not resolve, the app is silent and every
        // screen still looks right.
        assertNotNull(koin.get<ChimeScheduler>())
        assertNotNull(koin.get<ChimeCoordinator>())
    }
}
