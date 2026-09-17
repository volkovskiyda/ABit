package com.gmail.volkovskiyda.abit.app.shared

import com.gmail.volkovskiyda.abit.core.common.markFirebaseInitialised
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.initialize

/** The browser SDK takes the same public identifiers the Firebase console hands out for a web app. */
actual fun initFirebase(): Boolean =
    runCatching {
        Firebase.initialize(
            context = Unit,
            options =
                FirebaseOptions(
                    applicationId = FirebaseConfig.APP_ID_WEB,
                    apiKey = FirebaseConfig.API_KEY_WEB,
                    projectId = FirebaseConfig.PROJECT_ID,
                    authDomain = FirebaseConfig.AUTH_DOMAIN,
                    storageBucket = FirebaseConfig.STORAGE_BUCKET,
                    gcmSenderId = FirebaseConfig.PROJECT_NUMBER,
                ),
        )
        true
    }.getOrDefault(false).also(::markFirebaseInitialised)
