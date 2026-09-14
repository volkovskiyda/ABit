package com.gmail.volkovskiyda.abit.app.shared

import com.gmail.volkovskiyda.abit.core.common.firebaseAvailable

/**
 * Nothing to do: the Google Services plugin turns `google-services.json` into resources that
 * `FirebaseInitProvider` reads before `Application.onCreate`. With no such file there are no
 * resources and no `FirebaseApp`, which is exactly what [firebaseAvailable] reports.
 */
actual fun initFirebase(): Boolean = firebaseAvailable()
