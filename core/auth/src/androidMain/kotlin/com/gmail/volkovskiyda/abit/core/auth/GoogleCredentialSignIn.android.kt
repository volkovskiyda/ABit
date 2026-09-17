package com.gmail.volkovskiyda.abit.core.auth

import com.gmail.volkovskiyda.abit.core.domain.AuthUser
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseUser

/** The SDK here implements credential sign-in, so there is nothing to work around. */
internal actual suspend fun FirebaseAuth.signInWithGoogleCredential(
    idToken: String,
    link: FirebaseUser?,
): AuthUser = sdkSignInWithGoogleCredential(idToken, link)
