package com.gmail.volkovskiyda.abit.core.sync

import com.gmail.volkovskiyda.abit.core.domain.SyncState
import com.gmail.volkovskiyda.abit.core.domain.SyncStatusRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** The standing answer until Firestore is wired in (plan item 09): the app works, offline. */
object UnavailableSyncStatusRepository : SyncStatusRepository {
    override val syncState: Flow<SyncState> = flowOf(SyncState.Unavailable)

    override suspend fun syncNow() = Unit
}
