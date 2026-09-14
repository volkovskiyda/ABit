package com.gmail.volkovskiyda.abit.app.shared

import com.gmail.volkovskiyda.abit.core.common.DispatcherProvider
import com.gmail.volkovskiyda.abit.core.domain.PomodoroSessionRepository
import com.gmail.volkovskiyda.abit.core.domain.SyncStatusRepository
import com.gmail.volkovskiyda.abit.core.observability.CrashReporter
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.verify.verify
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Two checks over the object graph, both of which would otherwise only fail on a device.
 *
 * Koin binds by type at runtime, so a constructor parameter nobody provides is a crash at
 * injection rather than a compile error. These run on both JVM hosts in CI.
 */
class KoinGraphTest {

    @AfterTest
    fun tearDown() = stopKoin()

    /**
     * Verified as one merged module rather than one at a time: `verify()` only sees the definitions
     * of the module it is called on, and this graph is deliberately split so that (say) the
     * pomodoro ViewModel's repositories come from `core:data` and `core:sync`.
     */
    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `every declared dependency has a definition`() {
        module { includes(abitModules) }.verify()
    }

    /** The composition root the four apps call. Resolves the singletons a host actually asks for. */
    @Test
    fun `the composition root starts and resolves`() {
        val koin = initKoin().koin

        assertNotNull(koin.get<DispatcherProvider>())
        assertNotNull(koin.get<PomodoroSessionRepository>())
        assertNotNull(koin.get<SyncStatusRepository>())
        assertNotNull(koin.get<CrashReporter>())
    }
}
