package com.gmail.volkovskiyda.abit.core.data

import com.gmail.volkovskiyda.abit.core.database.dao.DayOverrideDao
import com.gmail.volkovskiyda.abit.core.database.dao.ScheduleDao
import com.gmail.volkovskiyda.abit.core.database.model.DayOverrideEntity
import com.gmail.volkovskiyda.abit.core.database.model.ScheduleEntity
import com.gmail.volkovskiyda.abit.core.database.model.toEntity
import com.gmail.volkovskiyda.abit.core.domain.AuthUser
import com.gmail.volkovskiyda.abit.core.domain.SyncState
import com.gmail.volkovskiyda.abit.core.model.DayOverride
import com.gmail.volkovskiyda.abit.core.model.Schedule
import com.gmail.volkovskiyda.abit.core.model.ScheduleId
import com.gmail.volkovskiyda.abit.core.model.UserId
import com.gmail.volkovskiyda.abit.core.sync.ScheduleRemoteSource
import com.gmail.volkovskiyda.abit.core.testing.FakeAuthRepository
import com.gmail.volkovskiyda.abit.core.testing.FakeTimeProvider
import com.gmail.volkovskiyda.abit.core.testing.FakeTimeZoneProvider
import com.gmail.volkovskiyda.abit.core.testing.TEST_EPOCH
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class SyncEngineTest {
    @Test
    fun `a newer remote schedule overwrites the local one`() =
        runTest {
            val fixture = fixture(local = listOf(schedule(name = "Old", updatedAt = TEST_EPOCH)))
            fixture.remote.schedules.value = listOf(schedule(name = "New", updatedAt = TEST_EPOCH + 1.hours))

            fixture.engine.syncNow()

            assertEquals("New", fixture.scheduleDao.findById("workdays")?.name)
        }

    @Test
    fun `an older remote schedule does not overwrite the local one`() =
        runTest {
            val fixture = fixture(local = listOf(schedule(name = "Local", updatedAt = TEST_EPOCH + 1.hours)))
            fixture.remote.schedules.value = listOf(schedule(name = "Stale", updatedAt = TEST_EPOCH))

            fixture.engine.syncNow()

            assertEquals("Local", fixture.scheduleDao.findById("workdays")?.name)
            assertEquals(
                "Local",
                fixture.remote.schedules.value
                    .single()
                    .name,
                "the newer local row is pushed",
            )
        }

    @Test
    fun `a remote tombstone removes the schedule from the observed list`() =
        runTest {
            val fixture = fixture(local = listOf(schedule(updatedAt = TEST_EPOCH)))
            fixture.remote.schedules.value =
                listOf(schedule(updatedAt = TEST_EPOCH + 1.hours, deletedAt = TEST_EPOCH + 1.hours))

            fixture.engine.syncNow()

            assertEquals(emptyList(), fixture.scheduleDao.observeAll().first())
            assertEquals(1, fixture.scheduleDao.allIncludingDeleted().size, "the tombstone itself stays")
        }

    @Test
    fun `a local tombstone newer than a live remote row wins`() =
        runTest {
            val deletedAt = TEST_EPOCH + 2.hours
            val fixture = fixture(local = listOf(schedule(updatedAt = deletedAt, deletedAt = deletedAt)))
            fixture.remote.schedules.value = listOf(schedule(name = "Alive", updatedAt = TEST_EPOCH))

            fixture.engine.syncNow()

            assertEquals(
                deletedAt,
                fixture.remote.schedules.value
                    .single()
                    .deletedAt,
            )
            assertEquals(emptyList(), fixture.scheduleDao.observeAll().first())
        }

    @Test
    fun `a day override reaches the remote`() =
        runTest {
            val date = LocalDate(2026, 9, 14)
            val fixture = fixture()
            fixture.dayOverrideDao.upsert(
                DayOverride(date = date, paused = true, updatedAt = TEST_EPOCH).toEntity(),
            )

            fixture.engine.syncNow()

            assertEquals(
                listOf(date),
                fixture.remote.overrides.value
                    .map { it.date },
            )
            assertTrue(
                fixture.remote.overrides.value
                    .single()
                    .paused,
            )
        }

    @Test
    fun `an anonymous user stays local and makes no remote call`() =
        runTest {
            val fixture = fixture(user = AuthUser(UserId("anon"), isAnonymous = true))

            fixture.engine.syncNow()

            assertEquals(SyncState.LocalOnly, fixture.engine.syncState.first())
            assertEquals(0, fixture.remote.calls)
        }

    @Test
    fun `a signed-out user reports SignedOut and makes no remote call`() =
        runTest {
            val fixture = fixture(user = null)

            fixture.engine.syncNow()

            assertEquals(SyncState.SignedOut, fixture.engine.syncState.first())
            assertEquals(0, fixture.remote.calls)
        }

    @Test
    fun `a build with no Firebase reports Unavailable and makes no remote call`() =
        runTest {
            val fixture = fixture(firebaseAvailable = false)

            fixture.engine.syncNow()

            assertEquals(SyncState.Unavailable, fixture.engine.syncState.first())
            assertEquals(0, fixture.remote.calls)
        }

    private class Fixture(
        val engine: SyncEngine,
        val remote: FakeScheduleRemoteSource,
        val scheduleDao: FakeScheduleDao,
        val dayOverrideDao: FakeDayOverrideDao,
    )

    private fun TestScope.fixture(
        local: List<Schedule> = emptyList(),
        user: AuthUser? = AuthUser(UserId("google-user"), isAnonymous = false),
        firebaseAvailable: Boolean = true,
    ): Fixture {
        val scheduleDao = FakeScheduleDao(local.map { it.toEntity() })
        val dayOverrideDao = FakeDayOverrideDao()
        val remote = FakeScheduleRemoteSource()
        val engine =
            SyncEngine(
                scheduleDao = scheduleDao,
                dayOverrideDao = dayOverrideDao,
                remote = remote,
                authRepository = FakeAuthRepository(user),
                timeProvider = FakeTimeProvider(),
                timeZoneProvider = FakeTimeZoneProvider(),
                scope = this,
                deviceId = "test-device",
                isFirebaseAvailable = { firebaseAvailable },
            )
        return Fixture(engine, remote, scheduleDao, dayOverrideDao)
    }

    private fun schedule(
        id: String = "workdays",
        name: String = "Workdays",
        updatedAt: Instant = TEST_EPOCH,
        deletedAt: Instant? = null,
    ) = Schedule(
        id = ScheduleId(id),
        name = name,
        enabled = true,
        days = setOf(DayOfWeek.MONDAY),
        start = LocalTime(9, 0),
        end = LocalTime(18, 0),
        focusMinutes = 45,
        breakMinutes = 15,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}

/** In-memory stand-ins for the Room DAOs. The DAOs are interfaces, so no database is needed here. */
private class FakeScheduleDao(
    initial: List<ScheduleEntity> = emptyList(),
) : ScheduleDao {
    private val rows = MutableStateFlow(initial)

    override fun observeAll(): Flow<List<ScheduleEntity>> =
        rows.asStateFlow().map { all ->
            all.filter { it.deletedAtMillis == null }.sortedWith(compareBy({ it.startMinuteOfDay }, { it.name }))
        }

    override suspend fun allIncludingDeleted(): List<ScheduleEntity> = rows.value

    override suspend fun findById(id: String): ScheduleEntity? = rows.value.firstOrNull { it.id == id }

    override suspend fun upsert(schedule: ScheduleEntity) {
        rows.update { current -> current.filterNot { it.id == schedule.id } + schedule }
    }

    override suspend fun upsertAll(schedules: List<ScheduleEntity>) = schedules.forEach { upsert(it) }

    override suspend fun purgeTombstones(before: Long) {
        rows.update { current ->
            current.filterNot { row -> row.deletedAtMillis?.let { it < before } == true }
        }
    }
}

private class FakeDayOverrideDao : DayOverrideDao {
    private val rows = MutableStateFlow<List<DayOverrideEntity>>(emptyList())

    override fun observeFrom(from: Long): Flow<List<DayOverrideEntity>> =
        rows.asStateFlow().map { all -> all.filter { it.epochDay >= from }.sortedBy { it.epochDay } }

    override suspend fun all(): List<DayOverrideEntity> = rows.value

    override suspend fun findByEpochDay(epochDay: Long): DayOverrideEntity? = rows.value.firstOrNull { it.epochDay == epochDay }

    override suspend fun upsert(override: DayOverrideEntity) {
        rows.update { current -> current.filterNot { it.epochDay == override.epochDay } + override }
    }

    override suspend fun upsertAll(overrides: List<DayOverrideEntity>) = overrides.forEach { upsert(it) }

    override suspend fun purgeBefore(before: Long) {
        rows.update { current -> current.filterNot { it.epochDay < before } }
    }
}

private class FakeScheduleRemoteSource : ScheduleRemoteSource {
    val schedules = MutableStateFlow<List<Schedule>>(emptyList())
    val overrides = MutableStateFlow<List<DayOverride>>(emptyList())

    /** Every call that would have reached Firestore, so a test can assert that none did. */
    var calls: Int = 0
        private set

    override fun observeSchedules(user: UserId): Flow<List<Schedule>> {
        calls++
        return schedules.asStateFlow()
    }

    override fun observeOverrides(
        user: UserId,
        from: LocalDate,
    ): Flow<List<DayOverride>> {
        calls++
        return overrides.asStateFlow().map { all -> all.filter { it.date >= from } }
    }

    override suspend fun upsertSchedule(
        user: UserId,
        schedule: Schedule,
        deviceId: String,
    ) {
        calls++
        schedules.update { current -> current.filterNot { it.id == schedule.id } + schedule }
    }

    override suspend fun upsertOverride(
        user: UserId,
        override: DayOverride,
        deviceId: String,
    ) {
        calls++
        overrides.update { current -> current.filterNot { it.date == override.date } + override }
    }
}
