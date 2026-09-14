package com.gmail.volkovskiyda.abit.core.database

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.cash.turbine.test
import com.gmail.volkovskiyda.abit.core.database.dao.PomodoroSessionDao
import com.gmail.volkovskiyda.abit.core.database.model.PomodoroSessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Runs against a real SQLite engine, in memory. It lives in `nonAndroidTest` rather than
 * `commonTest` because Room's bundled SQLite ships a JNI library that an Android *host* unit test
 * cannot load; the Android instrumented twin covers the same ground on a device.
 */
class PomodoroSessionDaoTest {
    private lateinit var database: AbitDatabase
    private lateinit var dao: PomodoroSessionDao

    @BeforeTest
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder<AbitDatabase>()
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.Default)
                .build()
        dao = database.pomodoroSessionDao()
    }

    @AfterTest
    fun tearDown() = database.close()

    @Test
    fun `upsert inserts then replaces the same row`() =
        runTest {
            dao.upsert(session(id = "a", completed = false))
            assertEquals(false, dao.findById("a")?.completed)

            dao.upsert(session(id = "a", completed = true))

            assertEquals(true, dao.findById("a")?.completed)
            assertEquals(1, dao.observeAllOnce().size)
        }

    @Test
    fun `observeAll emits newest first and updates on write`() =
        runTest {
            dao.upsert(session(id = "older", startedAtMillis = 1_000))
            dao.upsert(session(id = "newer", startedAtMillis = 2_000))

            dao.observeAll().test {
                assertEquals(listOf("newer", "older"), awaitItem().map { it.id })

                dao.upsert(session(id = "newest", startedAtMillis = 3_000))

                assertEquals(listOf("newest", "newer", "older"), awaitItem().map { it.id })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `deleteById removes only that row`() =
        runTest {
            dao.upsert(session(id = "keep"))
            dao.upsert(session(id = "drop"))

            dao.deleteById("drop")

            assertNull(dao.findById("drop"))
            assertEquals(listOf("keep"), dao.observeAllOnce().map { it.id })
        }

    private suspend fun PomodoroSessionDao.observeAllOnce(): List<PomodoroSessionEntity> {
        var result: List<PomodoroSessionEntity> = emptyList()
        observeAll().test {
            result = awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        return result
    }

    private fun session(
        id: String,
        startedAtMillis: Long = 1_700_000_000_000,
        completed: Boolean = true,
    ) = PomodoroSessionEntity(
        id = id,
        startedAtMillis = startedAtMillis,
        durationMinutes = 25,
        completed = completed,
        updatedAtMillis = startedAtMillis,
    )
}
