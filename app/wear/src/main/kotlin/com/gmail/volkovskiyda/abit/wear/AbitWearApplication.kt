package com.gmail.volkovskiyda.abit.wear

import android.app.Application
import com.gmail.volkovskiyda.abit.app.shared.AbitPlatform
import com.gmail.volkovskiyda.abit.app.shared.initKoin
import com.gmail.volkovskiyda.abit.app.shared.startChimes
import com.gmail.volkovskiyda.abit.app.shared.startSync
import com.gmail.volkovskiyda.abit.core.common.firebaseAvailable
import com.gmail.volkovskiyda.abit.core.common.initFirebaseAvailability
import com.gmail.volkovskiyda.abit.wear.di.wearModule
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level

class AbitWearApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Before Koin: the observability bindings ask whether Firebase initialised while the graph
        // is still being built, and that question needs a Context.
        initFirebaseAvailability(this)
        configureFirebaseCollection()
        initKoin(
            platform = AbitPlatform.Wear,
            // The watch shares the phone's applicationId and version, so the platform label is
            // the only thing that tells the two apart in a session list.
            versionName = BuildConfig.VERSION_NAME,
            platformModules = listOf(wearModule),
        ) {
            androidContext(this@AbitWearApplication)
            if (BuildConfig.DEBUG) androidLogger(Level.INFO)
        }.startSync().startChimes()
    }

    /**
     * Reporting is release-only. A developer's crashes and a debug build's timings would otherwise
     * mix into the numbers a release is judged on, and both SDKs persist this choice, so it has to
     * be set on every start rather than once.
     */
    private fun configureFirebaseCollection() {
        if (!firebaseAvailable()) return
        val collect = !BuildConfig.DEBUG
        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = collect
        FirebasePerformance.getInstance().isPerformanceCollectionEnabled = collect
    }
}
