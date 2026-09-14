package com.gmail.volkovskiyda.abit.wear

import android.app.Application
import com.gmail.volkovskiyda.abit.app.shared.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level

class AbitWearApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@AbitWearApplication)
            if (BuildConfig.DEBUG) androidLogger(Level.INFO)
        }
    }
}
