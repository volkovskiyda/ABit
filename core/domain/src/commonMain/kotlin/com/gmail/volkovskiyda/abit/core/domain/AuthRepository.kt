package com.gmail.volkovskiyda.abit.core.domain

import com.gmail.volkovskiyda.abit.core.model.UserId
import kotlinx.coroutines.flow.Flow

/** Who is signed in, if anyone. `null` means signed out entirely, not anonymous. */
data class AuthUser(
    val id: UserId,
    val isAnonymous: Boolean,
    val displayName: String? = null,
    val email: String? = null,
)

/**
 * Anonymous sign-in lets someone use the app with no account at all — sessions stay on the device.
 * Signing in with Google upgrades that same anonymous account rather than replacing it, so the
 * local history survives and starts syncing.
 */
interface AuthRepository {
    val currentUser: Flow<AuthUser?>

    suspend fun signInAnonymously(): Result<AuthUser>

    suspend fun signInWithGoogle(idToken: String): Result<AuthUser>

    suspend fun signOut()
}
