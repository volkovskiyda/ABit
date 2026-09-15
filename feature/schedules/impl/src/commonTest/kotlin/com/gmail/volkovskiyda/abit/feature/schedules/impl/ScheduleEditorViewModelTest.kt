package com.gmail.volkovskiyda.abit.feature.schedules.impl

import app.cash.turbine.test
import com.gmail.volkovskiyda.abit.core.domain.BlockKind
import com.gmail.volkovskiyda.abit.core.testing.FakeScheduleRepository
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
import kotlin.test.assertTrue

private val MONDAY = LocalDate(2026, 9, 14)

class ScheduleEditorViewModelTest {
    private val mainDispatcherRule = MainDispatcherRule()

    @BeforeTest
    fun setUp() = mainDispatcherRule.setUp()

    @AfterTest
    fun tearDown() = mainDispatcherRule.tearDown()

    @Test
    fun `the preview counts nine focus blocks for the design's own schedule`() =
        runTest {
            // Workdays 09:00-18:00 at 45/15. The mockups label this "8 sessions" on one screen and
            // "12 blocks" on another; both are illustrative placeholder numerals. The caption is
            // computed, so it is right where the screenshot is not.
            val viewModel = editor(id = "workdays", stored = listOf(testSchedule()))

            viewModel.state.test {
                val state = awaitItem()
                assertEquals(9, state.focusCount)
                assertEquals(17, state.blockCount, "nine focus blocks and eight breaks")
                assertEquals(LocalTime(17, 45), state.lastBlockEnds)
                assertEquals(
                    BlockKind.Focus,
                    state.previewPlan
                        ?.blocks
                        ?.last()
                        ?.kind,
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a blank name cannot be saved`() =
        runTest {
            val viewModel = editor()

            viewModel.state.test {
                viewModel.edit { it.copy(name = "") }
                assertTrue(expectMostRecentItem().nameError)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `every validation rule the design states is enforced`() =
        runTest {
            val viewModel = editor()
            viewModel.edit { it.copy(name = "Deep work") }

            viewModel.state.test {
                assertTrue(expectMostRecentItem().canSave)

                viewModel.edit { it.copy(days = emptySet()) }
                assertTrue(expectMostRecentItem().daysError)
                viewModel.edit { it.copy(days = setOf(DayOfWeek.MONDAY)) }

                viewModel.edit { it.copy(end = LocalTime(8, 0)) }
                assertTrue(expectMostRecentItem().hoursError)
                viewModel.edit { it.copy(end = LocalTime(18, 0)) }

                viewModel.edit { it.copy(focusMinutes = 3) }
                assertTrue(expectMostRecentItem().focusError, "below the 5-minute floor")
                viewModel.edit { it.copy(focusMinutes = 47) }
                assertTrue(expectMostRecentItem().focusError, "not a 5-minute step")
                viewModel.edit { it.copy(focusMinutes = 45) }

                viewModel.edit { it.copy(breakMinutes = 200) }
                assertTrue(expectMostRecentItem().breakError, "above the 120-minute ceiling")
                viewModel.edit { it.copy(breakMinutes = 15) }

                assertTrue(expectMostRecentItem().canSave, "every rule satisfied again")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `an overlap with another schedule is reported while editing`() =
        runTest {
            val viewModel =
                editor(
                    id = "b",
                    stored =
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
                val conflict = expectMostRecentItem().conflict
                assertEquals(LocalTime(17, 0), conflict?.from)
                assertEquals(LocalTime(18, 0), conflict?.to)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `saving writes the draft through`() =
        runTest {
            val repository = FakeScheduleRepository()
            val viewModel = ScheduleEditorViewModel(null, repository, fixedClock(MONDAY, LocalTime(9, 0)))
            viewModel.edit { it.copy(name = "Deep work") }

            viewModel.save()

            repository.observeSchedules().test {
                assertEquals("Deep work", awaitItem().single().name)
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun editor(
        id: String? = null,
        stored: List<com.gmail.volkovskiyda.abit.core.model.Schedule> = emptyList(),
    ) = ScheduleEditorViewModel(id, FakeScheduleRepository(stored), fixedClock(MONDAY, LocalTime(9, 0)))
}
