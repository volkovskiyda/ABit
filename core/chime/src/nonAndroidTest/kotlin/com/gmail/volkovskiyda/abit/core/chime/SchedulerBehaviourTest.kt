package com.gmail.volkovskiyda.abit.core.chime

import androidx.datastore.core.DataStore
import com.gmail.volkovskiyda.abit.core.common.LocalClock
import com.gmail.volkovskiyda.abit.core.common.TimeProvider
import com.gmail.volkovskiyda.abit.core.datastore.ChimeSound
import com.gmail.volkovskiyda.abit.core.datastore.UserPreferences
import com.gmail.volkovskiyda.abit.core.datastore.UserPreferencesRepository
import com.gmail.volkovskiyda.abit.core.domain.Block
import com.gmail.volkovskiyda.abit.core.domain.BlockKind
import com.gmail.volkovskiyda.abit.core.domain.Chime
import com.gmail.volkovskiyda.abit.core.domain.ChimeKind
import com.gmail.volkovskiyda.abit.core.domain.Session
import com.gmail.volkovskiyda.abit.core.model.Schedule
import com.gmail.volkovskiyda.abit.core.model.ScheduleId
import com.gmail.volkovskiyda.abit.core.testing.FakeDayOverrideRepository
import com.gmail.volkovskiyda.abit.core.testing.FakeLogger
import com.gmail.volkovskiyda.abit.core.testing.FakeScheduleRepository
import com.gmail.volkovskiyda.abit.core.testing.FakeTimeZoneProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

private val START: Instant = Instant.fromEpochSeconds(1_700_000_000)

/** The all-day, ten-minutes-a-block schedule [SchedulerBehaviourTest] re-arms against. */
private const val TEN_MINUTE_BLOCK = 10
private val DAY_START = LocalTime(0, 0)
private val DAY_END = LocalTime(23, 59)

/** Twenty minutes of a ten-minute grid, which is what the re-arm test advances through. */
private const val BOUNDARIES_IN_TWENTY_MINUTES = 2

/**
 * The wall clock this test controls. It tracks `runTest`'s virtual time so the polling loop makes
 * progress, and [skew] is the lid-was-closed case: real time moved while the coroutine did not.
 */
private class VirtualClock(
    private val virtualMillis: () -> Long,
) : TimeProvider {
    var skew: Duration = Duration.ZERO

    override fun now(): Instant = START + virtualMillis().milliseconds + skew
}

private class RecordingBell : Bell {
    var rings: Int = 0
        private set

    override suspend fun ring(sound: ChimeSound) {
        if (sound != ChimeSound.Silent) rings++
    }
}

private class PollingPreferencesStore : DataStore<UserPreferences> {
    private val state = MutableStateFlow(UserPreferences(chimeSound = ChimeSound.SoftBell))

    override val data: Flow<UserPreferences> = state.asStateFlow()

    override suspend fun updateData(transform: suspend (UserPreferences) -> UserPreferences): UserPreferences =
        transform(state.value).also { state.value = it }
}

/**
 * Lives in `nonAndroidTest` because [PollingChimeScheduler] is the desktop-and-web half of the chime
 * engine — Android delegates to `AlarmManager` and has nothing of this shape to test.
 */
class SchedulerBehaviourTest {
    @Test
    fun `rings once, at the boundary`() =
        runTest {
            val f = fixture(boundaryIn = 10.minutes)

            f.scheduler.arm(f.chime)
            advanceTimeBy(9.minutes)
            assertEquals(0, f.bell.rings, "it must not ring early")

            advanceTimeBy(2.minutes)
            assertEquals(1, f.bell.rings)
        }

    @Test
    fun `a clock that jumped past the boundary rings on the next wake`() =
        runTest {
            val f = fixture(boundaryIn = 10.minutes)

            f.scheduler.arm(f.chime)
            // The lid was shut: real time moved ten minutes while the coroutine slept through one.
            f.clock.skew = 10.minutes
            advanceTimeBy(1.minutes + 1.milliseconds)

            assertEquals(1, f.bell.rings)
        }

    @Test
    fun `a boundary more than five minutes stale is not rung`() =
        runTest {
            val f = fixture(boundaryIn = 10.minutes)

            f.scheduler.arm(f.chime)
            // Away for an hour. Chiming for a break that ended long ago is noise, not a reminder.
            f.clock.skew = 60.minutes
            advanceTimeBy(1.minutes + 1.milliseconds)

            assertEquals(0, f.bell.rings)
        }

    @Test
    fun `arming twice leaves one job, so a boundary rings once`() =
        runTest {
            val f = fixture(boundaryIn = 10.minutes)

            f.scheduler.arm(f.chime)
            f.scheduler.arm(f.chime)
            advanceTimeBy(11.minutes)

            assertEquals(1, f.bell.rings)
        }

    @Test
    fun `the boundary after the one that rang is armed, not lost`() =
        runTest {
            // The coordinator arms the next boundary from inside the job that just rang, so the
            // scheduler is re-entered on its own coroutine. Cancel-and-joining it there cancels the
            // caller before the relaunch, and desktop and web fell silent after one chime for the
            // rest of the session. A schedule is needed for this one: with no schedules the
            // coordinator disarms instead and there is no second boundary to miss.
            val f = fixture(boundaryIn = 10.minutes, schedules = listOf(everyTenMinutesToday()))

            f.scheduler.arm(f.chime)
            advanceTimeBy(11.minutes)
            assertEquals(1, f.bell.rings)

            // Two more boundaries in the next twenty minutes, so it is not "re-armed once" either:
            // every firing has to arm the one after it. With the self-cancel the count stays at 1.
            advanceTimeBy(20.minutes)
            assertEquals(BOUNDARIES_IN_TWENTY_MINUTES + 1, f.bell.rings, "the boundaries the coordinator armed never fired")
        }

    @Test
    fun `disarm stops a pending boundary`() =
        runTest {
            val f = fixture(boundaryIn = 10.minutes)

            f.scheduler.arm(f.chime)
            advanceTimeBy(1.minutes)
            f.scheduler.disarm()
            advanceTimeBy(20.minutes)

            assertEquals(0, f.bell.rings)
        }

    private class Fixture(
        val scheduler: PollingChimeScheduler,
        val bell: RecordingBell,
        val clock: VirtualClock,
        val chime: Chime,
    )

    /** Boundaries every ten minutes, all day, on whichever weekday [START] falls on. */
    private fun everyTenMinutesToday(): Schedule =
        Schedule(
            id = ScheduleId("every-ten"),
            name = "Workdays",
            enabled = true,
            days = setOf(START.toLocalDateTime(TimeZone.UTC).dayOfWeek),
            start = DAY_START,
            end = DAY_END,
            focusMinutes = TEN_MINUTE_BLOCK,
            breakMinutes = TEN_MINUTE_BLOCK,
            updatedAt = START,
        )

    private fun TestScope.fixture(
        boundaryIn: Duration,
        schedules: List<Schedule> = emptyList(),
    ): Fixture {
        val clock = VirtualClock { testScheduler.currentTime }
        val localClock = LocalClock(clock, FakeTimeZoneProvider())
        val bell = RecordingBell()
        val preferences = UserPreferencesRepository(PollingPreferencesStore(), backgroundScope, FakeLogger())

        lateinit var scheduler: PollingChimeScheduler
        val coordinator =
            ChimeCoordinator(
                scheduleRepository = FakeScheduleRepository(schedules),
                dayOverrideRepository = FakeDayOverrideRepository(),
                preferencesRepository = preferences,
                scheduler = object : ChimeScheduler by LazyScheduler({ scheduler }) {},
                clock = localClock,
                scope = backgroundScope,
            )
        scheduler =
            PollingChimeScheduler(
                scope = backgroundScope,
                clock = localClock,
                preferences = preferences,
                bell = bell,
                coordinator = { coordinator },
            )

        val at = (START + boundaryIn).toLocalDateTime(TimeZone.UTC)
        return Fixture(scheduler, bell, clock, chime(at))
    }

    private fun chime(at: LocalDateTime) =
        Chime(
            at = at,
            kind = ChimeKind.BreakStart,
            scheduleName = "Workdays",
            session =
                Session(
                    index = 0,
                    focus = Block(BlockKind.Focus, at.time, at.time),
                    rest = null,
                ),
        )
}

/** Breaks the constructor cycle between the coordinator and the scheduler it re-arms. */
private class LazyScheduler(
    private val delegate: () -> ChimeScheduler,
) : ChimeScheduler {
    override suspend fun arm(chime: Chime) = delegate().arm(chime)

    override suspend fun disarm() = delegate().disarm()

    override suspend fun showCountdown(state: com.gmail.volkovskiyda.abit.core.domain.TodayState) = delegate().showCountdown(state)
}
