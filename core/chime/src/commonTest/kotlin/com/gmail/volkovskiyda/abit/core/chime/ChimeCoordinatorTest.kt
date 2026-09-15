package com.gmail.volkovskiyda.abit.core.chime

import androidx.datastore.core.DataStore
import com.gmail.volkovskiyda.abit.core.common.LocalClock
import com.gmail.volkovskiyda.abit.core.datastore.UserPreferences
import com.gmail.volkovskiyda.abit.core.datastore.UserPreferencesRepository
import com.gmail.volkovskiyda.abit.core.domain.Chime
import com.gmail.volkovskiyda.abit.core.domain.TodayState
import com.gmail.volkovskiyda.abit.core.model.Schedule
import com.gmail.volkovskiyda.abit.core.model.ScheduleId
import com.gmail.volkovskiyda.abit.core.testing.FakeDayOverrideRepository
import com.gmail.volkovskiyda.abit.core.testing.FakeScheduleRepository
import com.gmail.volkovskiyda.abit.core.testing.FakeTimeProvider
import com.gmail.volkovskiyda.abit.core.testing.FakeTimeZoneProvider
import com.gmail.volkovskiyda.abit.core.testing.TEST_EPOCH
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

/**
 * [TEST_EPOCH] is 2023-11-14T22:13:20Z, a Tuesday, at 22:13 UTC — off hours for a 09:00–18:00
 * schedule, which is why every case here moves the clock to the hour it means.
 */
private val TUESDAY = LocalDate(2023, 11, 14)

class ChimeCoordinatorTest {
    @Test
    fun `arms the next boundary`() =
        runTest {
            val fixture = fixture(atHour = 9, atMinute = 22)

            fixture.coordinator.onChimeFired()

            assertEquals(
                LocalTime(9, 45),
                fixture.scheduler.armed
                    ?.at
                    ?.time,
            )
        }

    @Test
    fun `turning the chime off on this device disarms, without touching the plan`() =
        runTest {
            val fixture = fixture(atHour = 9, atMinute = 22)
            fixture.preferences.set(UserPreferences(chimeOnThisDevice = false))

            fixture.coordinator.onChimeFired()

            assertNull(fixture.scheduler.armed)
            assertEquals(1, fixture.scheduler.disarmCount)
            // The countdown still reports a running session: the device is silent, not off duty.
            val state = fixture.scheduler.lastState
            assertEquals(true, state is TodayState.Running)
        }

    @Test
    fun `pausing today arms tomorrow rather than nothing`() =
        runTest {
            val fixture = fixture(atHour = 9, atMinute = 22)
            fixture.overrides.setPaused(TUESDAY, paused = true)

            fixture.coordinator.onChimeFired()

            assertEquals(
                TUESDAY.plusOneDay(),
                fixture.scheduler.armed
                    ?.at
                    ?.date,
            )
            assertEquals(
                LocalTime(9, 0),
                fixture.scheduler.armed
                    ?.at
                    ?.time,
            )
        }

    @Test
    fun `a skipped boundary arms the one after it`() =
        runTest {
            val fixture = fixture(atHour = 9, atMinute = 22)
            fixture.overrides.skipBoundary(TUESDAY, LocalTime(9, 45))

            fixture.coordinator.onChimeFired()

            assertEquals(
                LocalTime(10, 0),
                fixture.scheduler.armed
                    ?.at
                    ?.time,
            )
        }

    @Test
    fun `no schedule at all means nothing is armed`() =
        runTest {
            val fixture = fixture(atHour = 9, atMinute = 22, schedules = emptyList())

            fixture.coordinator.onChimeFired()

            assertNull(fixture.scheduler.armed)
            assertEquals(1, fixture.scheduler.disarmCount)
        }

    private class Fixture(
        val coordinator: ChimeCoordinator,
        val scheduler: RecordingChimeScheduler,
        val overrides: FakeDayOverrideRepository,
        val preferences: FakePreferencesStore,
    )

    private fun TestScope.fixture(
        atHour: Int,
        atMinute: Int,
        schedules: List<Schedule> = listOf(workdays()),
    ): Fixture {
        // TEST_EPOCH is 22:13:20 UTC on the Tuesday; move it to the hour this case is about.
        val secondsFromMidnight = atHour * 3600L + atMinute * 60L
        val midnight = Instant.fromEpochSeconds(TEST_EPOCH.epochSeconds - (22 * 3600L + 13 * 60L + 20L))
        val time = FakeTimeProvider(Instant.fromEpochSeconds(midnight.epochSeconds + secondsFromMidnight))

        val scheduler = RecordingChimeScheduler()
        val overrides = FakeDayOverrideRepository(timeProvider = time)
        val preferences = FakePreferencesStore()
        return Fixture(
            coordinator =
                ChimeCoordinator(
                    scheduleRepository = FakeScheduleRepository(schedules, timeProvider = time),
                    dayOverrideRepository = overrides,
                    preferencesRepository = UserPreferencesRepository(preferences),
                    scheduler = scheduler,
                    clock = LocalClock(time, FakeTimeZoneProvider()),
                    scope = this,
                ),
            scheduler = scheduler,
            overrides = overrides,
            preferences = preferences,
        )
    }

    private fun workdays() =
        Schedule(
            id = ScheduleId("workdays"),
            name = "Workdays",
            enabled = true,
            days = DayOfWeek.entries.toSet(),
            start = LocalTime(9, 0),
            end = LocalTime(18, 0),
            focusMinutes = 45,
            breakMinutes = 15,
            updatedAt = TEST_EPOCH,
        )
}

private fun LocalDate.plusOneDay(): LocalDate = LocalDate.fromEpochDays(toEpochDays() + 1)

private class RecordingChimeScheduler : ChimeScheduler {
    var armed: Chime? = null
        private set
    var disarmCount: Int = 0
        private set
    var lastState: TodayState? = null
        private set

    override suspend fun arm(chime: Chime) {
        armed = chime
    }

    override suspend fun disarm() {
        armed = null
        disarmCount++
    }

    override suspend fun showCountdown(state: TodayState) {
        lastState = state
    }
}

/** A DataStore that is a `MutableStateFlow`, which is all [UserPreferencesRepository] needs. */
private class FakePreferencesStore : DataStore<UserPreferences> {
    private val state = MutableStateFlow(UserPreferences())

    override val data: Flow<UserPreferences> = state.asStateFlow()

    override suspend fun updateData(transform: suspend (UserPreferences) -> UserPreferences): UserPreferences =
        transform(state.value).also { state.value = it }

    suspend fun set(preferences: UserPreferences) {
        updateData { preferences }
    }
}
