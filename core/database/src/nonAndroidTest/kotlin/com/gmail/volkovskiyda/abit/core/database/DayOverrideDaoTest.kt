package com.gmail.volkovskiyda.abit.core.database

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.cash.turbine.test
import com.gmail.volkovskiyda.abit.core.database.dao.DayOverrideDao
import com.gmail.volkovskiyda.abit.core.database.model.DayOverrideEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// Epoch days around an arbitrary "today", named so the queries' cutoffs read as dates rather than
// as five-digit numbers.
private const val LAST_WEEK = 19_998L
private const val YESTERDAY = 19_999L
private const val TODAY = 20_000L
private const val TOMORROW = 20_001L
private const val DAY_AFTER = 20_002L

/** In `nonAndroidTest` for the reason spelled out in [ScheduleDaoTest]. */
class DayOverrideDaoTest {
    private lateinit var database: AbitDatabase
    private lateinit var dao: DayOverrideDao

    @BeforeTest
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder<AbitDatabase>()
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.Default)
                .build()
        dao = database.dayOverrideDao()
    }

    @AfterTest
    fun tearDown() = database.close()

    @Test
    fun `upsert inserts then replaces the same day`() =
        runTest {
            dao.upsert(override(epochDay = TODAY, paused = false))
            assertEquals(false, dao.findByEpochDay(TODAY)?.paused)

            dao.upsert(override(epochDay = TODAY, paused = true))

            assertEquals(true, dao.findByEpochDay(TODAY)?.paused)
            assertEquals(1, dao.all().size)
        }

    @Test
    fun `observeFrom excludes past days and re-emits on write`() =
        runTest {
            dao.upsertAll(
                listOf(
                    override(epochDay = YESTERDAY),
                    override(epochDay = TODAY),
                    override(epochDay = TOMORROW),
                ),
            )

            dao.observeFrom(TODAY).test {
                assertEquals(listOf(TODAY, TOMORROW), awaitItem().map { it.epochDay })

                dao.upsert(override(epochDay = DAY_AFTER))

                assertEquals(listOf(TODAY, TOMORROW, DAY_AFTER), awaitItem().map { it.epochDay })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `purgeBefore drops the days that can no longer matter`() =
        runTest {
            dao.upsertAll(listOf(override(epochDay = LAST_WEEK), override(epochDay = TODAY)))

            dao.purgeBefore(before = TODAY)

            assertNull(dao.findByEpochDay(LAST_WEEK))
            assertEquals(listOf(TODAY), dao.all().map { it.epochDay })
        }

    private fun override(
        epochDay: Long,
        paused: Boolean = true,
    ) = DayOverrideEntity(
        epochDay = epochDay,
        paused = paused,
        skippedBoundaries = "585,600",
        updatedAtMillis = 1_700_000_000_000,
    )
}
