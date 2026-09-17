package com.gmail.volkovskiyda.abit.feature.today.impl

import app.cash.turbine.test
import com.gmail.volkovskiyda.abit.core.domain.BlockKind
import com.gmail.volkovskiyda.abit.core.domain.TodayState
import com.gmail.volkovskiyda.abit.core.testing.FakeAuthRepository
import com.gmail.volkovskiyda.abit.core.testing.FakeDayOverrideRepository
import com.gmail.volkovskiyda.abit.core.testing.FakeScheduleRepository
import com.gmail.volkovskiyda.abit.core.testing.FakeSyncStatusRepository
import com.gmail.volkovskiyda.abit.core.testing.MainDispatcherRule
import com.gmail.volkovskiyda.abit.core.testing.fixedClock
import com.gmail.volkovskiyda.abit.core.testing.testSchedule
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** 2026-09-14 is a Monday, so the sample Workdays schedule runs on it. */
private val MONDAY = LocalDate(2026, 9, 14)

class TodayViewModelTest {
    private val mainDispatcherRule = MainDispatcherRule()

    @BeforeTest
    fun setUp() = mainDispatcherRule.setUp()

    @AfterTest
    fun tearDown() = mainDispatcherRule.tearDown()

    @Test
    fun `reports the running session and its countdown`() =
        runTest {
            val viewModel = viewModel(at = LocalTime(9, 22, 22))

            viewModel.state.test {
                val running = awaitItem().today
                assertIs<TodayState.Running>(running)
                assertEquals(BlockKind.Focus, running.stage)
                assertEquals(1, running.sessionNumber)
                assertEquals(9, running.sessionCount)
                assertEquals(22.minutes + 38.seconds, running.stageRemaining)
                assertEquals(37.minutes + 38.seconds, running.sessionRemaining)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `skipping today flips the state to Skipped without changing the plan`() =
        runTest {
            val overrides = FakeDayOverrideRepository()
            val viewModel = viewModel(at = LocalTime(9, 22), overrides = overrides)

            viewModel.state.test {
                assertIs<TodayState.Running>(awaitItem().today)

                viewModel.skipToday()

                val skipped = awaitItem().today
                assertIs<TodayState.Skipped>(skipped)
                assertEquals(9, skipped.plan.sessions.size, "skipping silences the day, it does not rewrite it")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `an overlapping pair surfaces an unresolved conflict`() =
        runTest {
            val viewModel =
                viewModel(
                    at = LocalTime(9, 22),
                    schedules =
                        listOf(
                            testSchedule(id = "a"),
                            testSchedule(
                                id = "b",
                                name = "Evening study",
                                days = setOf(DayOfWeek.MONDAY),
                                start = LocalTime(17, 0),
                                end = LocalTime(21, 30),
                            ),
                        ),
                )

            viewModel.state.test {
                val conflict = assertNotNull(awaitItem().unresolvedConflict)
                assertEquals(LocalTime(17, 0), conflict.from)
                assertEquals(LocalTime(18, 0), conflict.to)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `is off hours when no schedule runs, with nothing to conflict about`() =
        runTest {
            val viewModel = viewModel(at = LocalTime(9, 22), schedules = emptyList())

            viewModel.state.test {
                val state = awaitItem()
                assertIs<TodayState.OffHours>(state.today)
                assertNull(state.unresolvedConflict)
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun viewModel(
        at: LocalTime,
        schedules: List<com.gmail.volkovskiyda.abit.core.model.Schedule> = listOf(testSchedule()),
        overrides: FakeDayOverrideRepository = FakeDayOverrideRepository(),
    ) = TodayViewModel(
        scheduleRepository = FakeScheduleRepository(schedules),
        dayOverrideRepository = overrides,
        syncStatusRepository = FakeSyncStatusRepository(),
        authRepository = FakeAuthRepository(),
        clock = fixedClock(MONDAY, at),
    )
}
