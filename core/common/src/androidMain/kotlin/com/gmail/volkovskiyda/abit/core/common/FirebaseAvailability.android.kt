package com.gmail.volkovskiyda.abit.core.common

import android.content.Context
import com.google.firebase.FirebaseApp

/**
 * Set once from the Application, because this has to answer without a Koin lookup — the observability
 * bindings consult it while the graph is still being built.
 */
internal var applicationContext: Context? = null

/**
 * Firebase on Android initialises itself from the resources the Google Services plugin generates out
 * of `google-services.json`. With no such file there are no resources, `FirebaseApp.getApps` is
 * empty, and this returns false.
 */
actual fun firebaseAvailable(): Boolean {
    val context = applicationContext ?: return false
    return FirebaseApp.getApps(context).isNotEmpty()
}

/** Called from `Application.onCreate` before Koin starts. */
fun initFirebaseAvailability(context: Context) {
    applicationContext = context.applicationContext
}
