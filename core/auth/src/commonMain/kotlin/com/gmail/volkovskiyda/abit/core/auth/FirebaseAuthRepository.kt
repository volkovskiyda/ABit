package com.gmail.volkovskiyda.abit.core.auth

import com.gmail.volkovskiyda.abit.core.domain.AuthRepository
import com.gmail.volkovskiyda.abit.core.domain.AuthUser
import com.gmail.volkovskiyda.abit.core.model.UserId
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Firebase Authentication, as the app sees it.
 *
 * Anonymous first is the product decision: someone can use ABit with no account at all, and their
 * sessions stay on the device. Signing in with Google then **links** that anonymous account rather
 * than replacing it, so the history they already have survives and starts syncing. That linking
 * step is the whole reason this class exists instead of two calls at the call site.
 */
class FirebaseAuthRepository(
    private val auth: FirebaseAuth = Firebase.auth,
) : AuthRepository {
    override val currentUser: Flow<AuthUser?> =
        auth.authStateChanged.map { user -> user?.toAuthUser() }

    override suspend fun signInAnonymously(): Result<AuthUser> =
        runCatching {
            auth.signInAnonymously().user.requireUser()
        }

    /**
     * Upgrades the current anonymous account when there is one, and signs in normally otherwise.
     *
     * The collision case — the Google account already exists as a separate Firebase user — cannot
     * be resolved here: two accounts hold two sets of sessions and merging them is a product
     * question, not a plumbing one. This signs into the existing account, which is the behaviour
     * that loses nothing already synced, and the anonymous account's local data is left where it is.
     * Merging it is a backlog item on the infrastructure plan.
     */
    override suspend fun signInWithGoogle(idToken: String): Result<AuthUser> =
        runCatching {
            val credential = GoogleAuthProvider.credential(idToken = idToken, accessToken = null)
            val anonymous = auth.currentUser?.takeIf { it.isAnonymous }

            val user =
                if (anonymous != null) {
                    runCatching { anonymous.linkWithCredential(credential).user }
                        .getOrElse { auth.signInWithCredential(credential).user }
                } else {
                    auth.signInWithCredential(credential).user
                }
            user.requireUser()
        }

    override suspend fun signOut() = auth.signOut()

    private fun FirebaseUser?.requireUser(): AuthUser =
        checkNotNull(this?.toAuthUser()) { "Firebase returned no user for a successful sign-in." }
}

private fun FirebaseUser.toAuthUser(): AuthUser =
    AuthUser(
        id = UserId(uid),
        isAnonymous = isAnonymous,
        displayName = displayName,
        email = email,
    )
