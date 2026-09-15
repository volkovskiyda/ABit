package com.gmail.volkovskiyda.abit.core.sync

import com.gmail.volkovskiyda.abit.core.model.DayOverride
import com.gmail.volkovskiyda.abit.core.model.Schedule
import com.gmail.volkovskiyda.abit.core.model.UserId
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

/**
 * The remote half of sync: one user's schedules and day overrides in Firestore, as flows that update
 * when any of their devices writes.
 *
 * It knows nothing about the local database — reconciling the two is [SyncEngine]'s job — so this
 * stays a thin, testable mapping onto Firestore's own API.
 */
class FirestoreScheduleRemoteSource(
    // Lazy, and deliberately so: Koin creates every `single` when the graph starts, while
    // `Firebase.firestore` throws unless Firebase initialised. Resolving it on first use instead
    // means a build with no credentials can still hold this binding and simply never call it.
    firestoreProvider: () -> FirebaseFirestore = { Firebase.firestore },
) : ScheduleRemoteSource {
    private val firestore: FirebaseFirestore by lazy(firestoreProvider)

    override fun observeSchedules(user: UserId): Flow<List<Schedule>> =
        schedules(user).snapshots.map { snapshot ->
            snapshot.documents.map { it.data(ScheduleDocument.serializer()).toModel() }
        }

    /**
     * Filtered here rather than with a `where` clause: the collection holds a handful of documents,
     * and a query on `epochDay` would need a composite index nobody would remember to deploy.
     */
    override fun observeOverrides(
        user: UserId,
        from: LocalDate,
    ): Flow<List<DayOverride>> =
        overrides(user).snapshots.map { snapshot ->
            snapshot.documents
                .map { it.data(DayOverrideDocument.serializer()).toModel() }
                .filter { it.date >= from }
        }

    override suspend fun upsertSchedule(
        user: UserId,
        schedule: Schedule,
        deviceId: String,
    ) {
        schedules(user)
            .document(schedule.id.value)
            .set(ScheduleDocument.serializer(), schedule.toDocument(deviceId))
    }

    override suspend fun upsertOverride(
        user: UserId,
        override: DayOverride,
        deviceId: String,
    ) {
        overrides(user)
            .document(override.documentId())
            .set(DayOverrideDocument.serializer(), override.toDocument(deviceId))
    }

    private fun schedules(user: UserId) = userDocument(user).collection(FirestorePaths.SCHEDULES)

    private fun overrides(user: UserId) = userDocument(user).collection(FirestorePaths.DAY_OVERRIDES)

    private fun userDocument(user: UserId) = firestore.collection(FirestorePaths.USERS).document(user.value)
}
