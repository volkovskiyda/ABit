package com.gmail.volkovskiyda.abit.app.shared

import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify
import kotlin.reflect.KClass
import kotlin.test.Test

/**
 * Types the graph legitimately expects from outside the modules. On Android that is
 * `android.content.Context`, which `initKoin { androidContext(…) }` registers at startup and which
 * `verify()` — running without starting Koin — therefore cannot see. Empty everywhere else.
 */
internal expect val koinVerifyExtraTypes: List<KClass<*>>

/**
 * Koin binds by type at runtime, so a constructor parameter nobody provides is a crash at injection
 * rather than a compile error. This turns it back into a build failure, on both JVM hosts in CI.
 *
 * Verified as one merged module rather than one at a time: `verify()` only sees the definitions of
 * the module it is called on, and this graph is deliberately split so that (say) the pomodoro
 * ViewModel's repositories come from `core:data` and `core:sync`.
 *
 * This checks the wiring without building anything. Actually starting the graph is
 * [CompositionRootTest], which needs a platform and so cannot run on the Android host JVM.
 */
class KoinGraphTest {
    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `every declared dependency has a definition`() {
        module { includes(abitModules) }.verify(extraTypes = koinVerifyExtraTypes)
    }
}
