package com.gmail.volkovskiyda.abit.app.shared

import android.app.Application
import com.gmail.volkovskiyda.abit.core.common.AppDirs
import com.gmail.volkovskiyda.abit.core.common.markFirebaseInitialised
import com.google.firebase.FirebasePlatform
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.initialize
import java.io.File
import java.util.Properties

/**
 * The JVM has no Firebase SDK of its own, so GitLive ships a port of the Android one. It cannot
 * assume a filesystem layout or a logger, and refuses to start until given both — which is what
 * [FirebasePlatform.initializeFirebasePlatform] supplies.
 *
 * Persisting to a file rather than memory is what keeps a signed-in user signed in across restarts:
 * the auth SDK stores its refresh token through exactly this interface.
 */
actual fun initFirebase(): Boolean =
    runCatching {
        FirebasePlatform.initializeFirebasePlatform(FilePlatform(AppDirs.dataDirectory))
        Firebase.initialize(
            // An android.app.Application, on the JVM, and not a mistake: the Firebase Java SDK is a
            // port of the Firebase *Android* SDK and ships its own stubs of those classes, which
            // GitLive's JVM binding casts to unconditionally. Neither Unit nor null survives the
            // cast — and a bare `Context` does not survive the *second* one: Firestore's
            // `AndroidConnectivityMonitor` casts the application context to `Application` to
            // register lifecycle callbacks, and a `ClassCastException` there is raised inside its
            // async queue, which rethrows it on the main thread as "Internal error in Cloud
            // Firestore". The listener never opens, so nothing syncs and nothing says why.
            context = Application(),
            options =
                FirebaseOptions(
                    applicationId = FirebaseConfig.APP_ID_WEB,
                    apiKey = FirebaseConfig.API_KEY_DESKTOP,
                    projectId = FirebaseConfig.PROJECT_ID,
                    authDomain = FirebaseConfig.AUTH_DOMAIN,
                    storageBucket = FirebaseConfig.STORAGE_BUCKET,
                    gcmSenderId = FirebaseConfig.PROJECT_NUMBER,
                ),
        )
        true
    }.getOrDefault(false).also(::markFirebaseInitialised)

/** Key-value storage and logging, backed by one properties file in the app's data directory. */
private class FilePlatform(
    directory: File,
) : FirebasePlatform() {
    private val file = File(directory, "firebase.properties")
    private val properties =
        Properties().apply {
            if (file.exists()) file.inputStream().use(::load)
        }

    override fun store(
        key: String,
        value: String,
    ) {
        properties.setProperty(key, value)
        flush()
    }

    override fun retrieve(key: String): String? = properties.getProperty(key)

    override fun clear(key: String) {
        properties.remove(key)
        flush()
    }

    override fun log(msg: String) = println("Firebase: $msg")

    private fun flush() = file.outputStream().use { properties.store(it, null) }
}
