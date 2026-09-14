package com.gmail.volkovskiyda.abit.app.shared

import com.gmail.volkovskiyda.abit.core.auth.di.authModule
import com.gmail.volkovskiyda.abit.core.common.di.commonModule
import com.gmail.volkovskiyda.abit.core.data.SyncEngine
import com.gmail.volkovskiyda.abit.core.data.di.dataModule
import com.gmail.volkovskiyda.abit.core.database.di.databaseModule
import com.gmail.volkovskiyda.abit.core.database.di.platformDatabaseModule
import com.gmail.volkovskiyda.abit.core.datastore.di.datastoreModule
import com.gmail.volkovskiyda.abit.core.datastore.di.platformDatastoreModule
import com.gmail.volkovskiyda.abit.core.observability.di.observabilityModule
import com.gmail.volkovskiyda.abit.core.observability.di.platformObservabilityModule
import com.gmail.volkovskiyda.abit.core.sync.di.syncModule
import com.gmail.volkovskiyda.abit.feature.pomodoro.impl.di.pomodoroModule
import io.kotzilla.generated.monitoring
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

/**
 * Every module in the object graph, in the order Koin resolves them. Order matters only for
 * overrides: a platform module passed to [initKoin] comes after these, so Android can replace the
 * no-op crash reporter with Crashlytics without this list knowing about it.
 */
val abitModules: List<Module> =
    listOf(
        commonModule,
        observabilityModule,
        platformObservabilityModule,
        platformDatabaseModule,
        databaseModule,
        platformDatastoreModule,
        datastoreModule,
        authModule,
        syncModule,
        dataModule,
        pomodoroModule,
    )

/**
 * The one composition root, shared by all four apps. Each platform's entry point calls it once —
 * `Application.onCreate` on Android and Wear, `main()` on desktop and the web — passing whatever
 * only that platform can provide (a `Context`, a file path, a window).
 *
 * `monitoring()` is Kotzilla's, and the call is deliberately unconditional and last: last because
 * it inspects the modules already registered, unconditional because a checkout without
 * `app/shared/kotzilla.json` compiles a no-op of the same name from `src/kotzillaDisabled`. Nothing
 * here has to know whether the keys are present.
 *
 * @param platformModules bindings that exist on one platform only, applied after [abitModules] so
 *   they override the common defaults.
 * @param config extra Koin configuration, e.g. `androidContext(this@AbitApplication)`.
 */
fun initKoin(
    platformModules: List<Module> = emptyList(),
    config: KoinAppDeclaration? = null,
): KoinApplication {
    // Before the graph: the auth and observability bindings ask whether Firebase initialised, and a
    // binding cannot answer that for itself.
    initFirebase()

    return startKoin {
        config?.invoke(this)
        modules(abitModules + platformModules)
        monitoring()
    }
}

/**
 * Starts the background sync loop. Separate from [initKoin] on purpose: building the object graph
 * and starting long-lived work are different decisions, and a test that wants the first without the
 * second should not have to unpick the second. Each app's entry point calls it once, after
 * [initKoin]; it is a no-op in a build with no Firebase.
 */
fun KoinApplication.startSync() {
    koin.get<SyncEngine>().start()
}
