package com.gmail.volkovskiyda.abit.core.common

/**
 * Always true: this platform has no `google-services.json` to be missing. It initialises Firebase
 * from the identifiers compiled into `FirebaseConfig`, which are committed because they are public.
 */
actual fun firebaseAvailable(): Boolean = true
