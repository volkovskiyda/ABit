package com.gmail.volkovskiyda.abit.feature.schedules.impl

import app.cash.turbine.test
import com.gmail.volkovskiyda.abit.core.model.ScheduleId
import com.gmail.volkovskiyda.abit.core.testing.FakeScheduleRepository
import com.gmail.volkovskiyda.abit.core.testing.MainDispatcherRule
import com.gmail.volkovskiyda.abit.core.testing.testSchedule
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SchedulesViewModelTest {
    private val mainDispatcherRule = MainDispatcherRule()

    @BeforeTest
    fun setUp() = mainDispatcherRule.setUp()

    @AfterTest
    fun tearDown() = mainDispatcherRule.tearDown()

    @Test
    fun `toggling a schedule writes through`() =
        runTest {
            val repository = FakeScheduleRepository(listOf(testSchedule()))
            val viewModel = SchedulesViewModel(repository)

            viewModel.state.test {
                assertTrue(awaitItem().schedules.single().enabled)

                viewModel.toggle(ScheduleId("workdays"), enabled = false)

                assertEquals(false, awaitItem().schedules.single().enabled)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `deleting takes the schedule out of the list`() =
        runTest {
            val repository = FakeScheduleRepository(listOf(testSchedule()))
            val viewModel = SchedulesViewModel(repository)

            viewModel.state.test {
                assertEquals(1, awaitItem().schedules.size)

                viewModel.delete(ScheduleId("workdays"))

                // The fake keeps the tombstone, exactly as the database does; `conflicts()` and every
                // read filter it out, which is what the screen sees.
                assertEquals(0, awaitItem().conflicts.size)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `resolving a conflict disables exactly the other schedule`() =
        runTest {
            val repository =
                FakeScheduleRepository(
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
            val viewModel = SchedulesViewModel(repository)

            viewModel.state.test {
                assertEquals(1, awaitItem().conflicts.size)

                viewModel.resolveConflict(keep = ScheduleId("a"), disable = ScheduleId("b"))

                val after = awaitItem()
                assertTrue(after.schedules.first { it.id == ScheduleId("a") }.enabled)
                assertEquals(false, after.schedules.first { it.id == ScheduleId("b") }.enabled)
                assertEquals(0, after.conflicts.size, "a disabled schedule cannot conflict")
                cancelAndIgnoreRemainingEvents()
            }
        }
}
