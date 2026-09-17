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
     * Two things can happen to an anonymous account here, and they are genuinely different:
     *
     * - **It links.** Firebase attaches the Google credential to the account that already exists,
     *   *keeping its uid*. Everything written anonymously simply belongs to a Google account now.
     * - **It collides.** That Google account is already a separate Firebase user, so the link is
     *   refused and there is no way to make one uid out of two. This signs into the existing
     *   account, which is the outcome that loses nothing already synced, and [discard]s the
     *   anonymous one on the way.
     *
     * The collision case leaves the schedules written anonymously on the device, and `SyncEngine`
     * is what carries them into the account — it can see this happened, because a link keeps the
     * uid and a collision changes it. Its KDoc has the merge rule; the short version is that the
     * account's own copy wins where both sides know a schedule, and anything only this device has
     * is added.
     */
    override suspend fun signInWithGoogle(idToken: String): Result<AuthUser> =
        runCatching {
            val credential = GoogleAuthProvider.credential(idToken = idToken, accessToken = null)
            val anonymous = auth.currentUser?.takeIf { it.isAnonymous }

            val user =
                if (anonymous != null) {
                    runCatching { anonymous.linkWithCredential(credential).user }
                        .getOrElse {
                            anonymous.discard()
                            auth.signInWithCredential(credential).user
                        }
                } else {
                    auth.signInWithCredential(credential).user
                }
            user.requireUser()
        }

    /**
     * Deletes the anonymous account that could not be linked, **before** signing into the Google
     * one.
     *
     * Before, because a `FirebaseUser` can only delete itself while it is the signed-in user, and
     * one sign-in later this object is stale. That ordering is safe here for one reason, and it is
     * worth being explicit about: an anonymous uid never syncs (see `SyncEngine`), so this account
     * holds nothing in Firestore. Deleting it destroys an identity nobody can sign back into, not
     * data — the schedules are in the local database either way.
     *
     * A failure is swallowed on purpose. Firebase refuses `delete()` on an account whose sign-in is
     * too old, and the only consequence is one unreachable anonymous row left in the project's user
     * list. Failing the sign-in over that would trade a real outcome for a tidy one.
     */
    private suspend fun FirebaseUser.discard() {
        runCatching { delete() }
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
