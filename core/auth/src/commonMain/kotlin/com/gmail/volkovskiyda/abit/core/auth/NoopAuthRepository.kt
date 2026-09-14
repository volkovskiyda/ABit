package com.gmail.volkovskiyda.abit.core.auth

import com.gmail.volkovskiyda.abit.core.domain.AuthRepository
import com.gmail.volkovskiyda.abit.core.domain.AuthUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * What a build with no Firebase credentials gets. Every call fails with the same message rather
 * than throwing, so the UI can say "sync unavailable in this build" instead of crashing — see the
 * keyless-build note in the README.
 */
object NoopAuthRepository : AuthRepository {
    override val currentUser: Flow<AuthUser?> = flowOf(null)

    override suspend fun signInAnonymously(): Result<AuthUser> = failure()

    override suspend fun signInWithGoogle(idToken: String): Result<AuthUser> = failure()

    override suspend fun signOut() = Unit

    private fun failure(): Result<AuthUser> = Result.failure(IllegalStateException("Firebase is not configured in this build."))
}
