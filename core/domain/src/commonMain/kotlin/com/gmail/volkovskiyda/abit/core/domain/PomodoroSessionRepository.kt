package com.gmail.volkovskiyda.abit.core.domain

import com.gmail.volkovskiyda.abit.core.model.PomodoroSession
import kotlinx.coroutines.flow.Flow

/**
 * Reads come from the local database on every platform, so a screen renders without waiting for the
 * network; `core:sync` is what reconciles that database with Firestore in the background.
 */
interface PomodoroSessionRepository {
    fun observeSessions(): Flow<List<PomodoroSession>>

    suspend fun upsert(session: PomodoroSession)

    suspend fun delete(id: String)
}
