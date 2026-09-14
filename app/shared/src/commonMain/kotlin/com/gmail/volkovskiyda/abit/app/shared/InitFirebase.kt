package com.gmail.volkovskiyda.abit.app.shared

/**
 * Brings Firebase up, once, before [initKoin].
 *
 * Android reads `google-services.json` and initialises itself, so its actual does nothing; the
 * desktop and web builds have no such file and pass [FirebaseConfig]'s committed identifiers by
 * hand. The desktop one additionally has to give the Firebase Java SDK somewhere to persist the
 * signed-in user, which is what keeps a desktop session alive across restarts.
 *
 * Returns false when Firebase could not be initialised. That is a supported state, not an error:
 * the app runs, keeps everything on the device, and reports sync as unavailable.
 */
expect fun initFirebase(): Boolean
