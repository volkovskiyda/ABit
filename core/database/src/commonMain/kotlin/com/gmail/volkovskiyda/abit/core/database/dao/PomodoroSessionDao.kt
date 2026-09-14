package com.gmail.volkovskiyda.abit.core.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.gmail.volkovskiyda.abit.core.database.model.PomodoroSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PomodoroSessionDao {
    /** Newest first, which is the order every screen wants and the only one worth an index. */
    @Query("SELECT * FROM pomodoro_sessions ORDER BY startedAtMillis DESC")
    fun observeAll(): Flow<List<PomodoroSessionEntity>>

    @Query("SELECT * FROM pomodoro_sessions WHERE id = :id")
    suspend fun findById(id: String): PomodoroSessionEntity?

    /** Upsert rather than insert-or-replace: sync writes the same row repeatedly. */
    @Upsert
    suspend fun upsert(session: PomodoroSessionEntity)

    @Query("DELETE FROM pomodoro_sessions WHERE id = :id")
    suspend fun deleteById(id: String)
}
