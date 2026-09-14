package com.gmail.volkovskiyda.abit

import android.app.Application
import com.gmail.volkovskiyda.abit.app.shared.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level

class AbitApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@AbitApplication)
            // Koin's own resolution logging, debug builds only: it names every definition it
            // creates, which is noise in a release build and costs time on every injection.
            if (BuildConfig.DEBUG) androidLogger(Level.INFO)
        }
    }
}
