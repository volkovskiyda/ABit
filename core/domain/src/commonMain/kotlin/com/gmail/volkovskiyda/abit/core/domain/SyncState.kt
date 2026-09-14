package com.gmail.volkovskiyda.abit.core.domain

import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

/**
 * What the sync indicator shows. [Unavailable] is not an error state: it is what a build without
 * Firebase credentials reports, which is every fresh clone of this repository and every pull
 * request build. The app is fully usable there, it simply keeps everything on the device.
 */
sealed interface SyncState {
    data object Unavailable : SyncState

    data object SignedOut : SyncState

    data object Syncing : SyncState

    data class Idle(
        val lastSyncedAt: Instant?,
    ) : SyncState

    data class Failed(
        val reason: String,
    ) : SyncState
}

interface SyncStatusRepository {
    val syncState: Flow<SyncState>

    suspend fun syncNow()
}
