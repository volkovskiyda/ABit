package com.gmail.volkovskiyda.abit.core.sync

import com.gmail.volkovskiyda.abit.core.model.PomodoroSession
import com.gmail.volkovskiyda.abit.core.model.UserId
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The remote half of sync: one user's sessions in Firestore, as a flow that updates when any of
 * their devices writes.
 *
 * It knows nothing about the local database — reconciling the two is [SyncEngine]'s job — so this
 * stays a thin, testable mapping onto Firestore's own API.
 */
class FirestoreSessionRemoteSource(
    // Lazy, and deliberately so: Koin creates every `single` when the graph starts, while
    // `Firebase.firestore` throws unless Firebase initialised. Resolving it on first use instead
    // means a build with no credentials can still hold this binding and simply never call it.
    firestoreProvider: () -> FirebaseFirestore = { Firebase.firestore },
) {
    private val firestore: FirebaseFirestore by lazy(firestoreProvider)

    fun observeSessions(user: UserId): Flow<List<PomodoroSession>> =
        sessions(user).snapshots.map { snapshot ->
            snapshot.documents.map { it.data(SessionDocument.serializer()).toModel() }
        }

    suspend fun upsert(
        user: UserId,
        session: PomodoroSession,
        deviceId: String,
    ) {
        sessions(user)
            .document(session.id)
            .set(SessionDocument.serializer(), session.toDocument(deviceId))
    }

    suspend fun delete(
        user: UserId,
        id: String,
    ) {
        sessions(user).document(id).delete()
    }

    private fun sessions(user: UserId) =
        firestore
            .collection(FirestorePaths.USERS)
            .document(user.value)
            .collection(FirestorePaths.SESSIONS)
}
