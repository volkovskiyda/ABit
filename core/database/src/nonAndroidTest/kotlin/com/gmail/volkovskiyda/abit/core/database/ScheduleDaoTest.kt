package com.gmail.volkovskiyda.abit.core.database

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.cash.turbine.test
import com.gmail.volkovskiyda.abit.core.database.dao.ScheduleDao
import com.gmail.volkovskiyda.abit.core.database.model.ScheduleEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val NINE_AM = 9 * 60

/**
 * Runs against a real SQLite engine, in memory. It lives in `nonAndroidTest` rather than
 * `commonTest` because Room's bundled SQLite ships a JNI library that an Android *host* unit test
 * cannot load; the Android instrumented twin covers the same ground on a device.
 */
class ScheduleDaoTest {
    private lateinit var database: AbitDatabase
    private lateinit var dao: ScheduleDao

    @BeforeTest
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder<AbitDatabase>()
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.Default)
                .build()
        dao = database.scheduleDao()
    }

    @AfterTest
    fun tearDown() = database.close()

    @Test
    fun `upsert inserts then replaces the same row`() =
        runTest {
            dao.upsert(schedule(id = "a", enabled = true))
            assertEquals(true, dao.findById("a")?.enabled)

            dao.upsert(schedule(id = "a", enabled = false))

            assertEquals(false, dao.findById("a")?.enabled)
            assertEquals(1, dao.observeAllOnce().size)
        }

    @Test
    fun `observeAll hides tombstones while allIncludingDeleted shows them`() =
        runTest {
            dao.upsertAll(
                listOf(
                    schedule(id = "live"),
                    schedule(id = "gone", deletedAtMillis = 1_700_000_000_000),
                ),
            )

            assertEquals(listOf("live"), dao.observeAllOnce().map { it.id })
            assertEquals(listOf("gone", "live"), dao.allIncludingDeleted().map { it.id }.sorted())
        }

    @Test
    fun `observeAll orders by start time then name`() =
        runTest {
            dao.upsertAll(
                listOf(
                    schedule(id = "c", name = "Zulu", startMinuteOfDay = 9 * 60),
                    schedule(id = "a", name = "Alpha", startMinuteOfDay = 17 * 60),
                    schedule(id = "b", name = "Bravo", startMinuteOfDay = 9 * 60),
                ),
            )

            assertEquals(listOf("Bravo", "Zulu", "Alpha"), dao.observeAllOnce().map { it.name })
        }

    @Test
    fun `observeAll re-emits on write`() =
        runTest {
            dao.upsert(schedule(id = "first"))

            dao.observeAll().test {
                assertEquals(listOf("first"), awaitItem().map { it.id })

                dao.upsert(schedule(id = "second", startMinuteOfDay = 20 * 60))

                assertEquals(listOf("first", "second"), awaitItem().map { it.id })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `findById does not return a soft-deleted schedule`() =
        runTest {
            // Deleted on another device while the editor key sat on the back stack. Handing it back
            // as an ordinary draft let the edit be saved with its deletedAt still on it, so the
            // schedule vanished from the list again as soon as it was written.
            dao.upsert(schedule(id = "deleted-elsewhere", deletedAtMillis = 1_000))

            assertNull(dao.findById("deleted-elsewhere"))
            assertEquals(listOf("deleted-elsewhere"), dao.allIncludingDeleted().map { it.id })
        }

    @Test
    fun `purgeTombstones removes only deleted rows older than the cutoff`() =
        runTest {
            dao.upsertAll(
                listOf(
                    schedule(id = "live"),
                    schedule(id = "old-tombstone", deletedAtMillis = 1_000),
                    schedule(id = "fresh-tombstone", deletedAtMillis = 9_000),
                ),
            )

            dao.purgeTombstones(before = 5_000)

            assertNull(dao.findById("old-tombstone"))
            assertEquals(
                listOf("fresh-tombstone", "live"),
                dao.allIncludingDeleted().map { it.id }.sorted(),
            )
        }

    private suspend fun ScheduleDao.observeAllOnce(): List<ScheduleEntity> {
        var result: List<ScheduleEntity> = emptyList()
        observeAll().test {
            result = awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        return result
    }

    @Suppress("LongParameterList")
    private fun schedule(
        id: String,
        name: String = "Workdays",
        enabled: Boolean = true,
        startMinuteOfDay: Int = NINE_AM,
        deletedAtMillis: Long? = null,
    ) = ScheduleEntity(
        id = id,
        name = name,
        enabled = enabled,
        daysMask = 0b0011111,
        startMinuteOfDay = startMinuteOfDay,
        endMinuteOfDay = 18 * 60,
        focusMinutes = 45,
        breakMinutes = 15,
        updatedAtMillis = 1_700_000_000_000,
        deletedAtMillis = deletedAtMillis,
    )
}
