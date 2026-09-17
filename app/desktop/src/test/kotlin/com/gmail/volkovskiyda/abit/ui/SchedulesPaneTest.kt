package com.gmail.volkovskiyda.abit.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.gmail.volkovskiyda.abit.core.designsystem.AbitTheme
import com.gmail.volkovskiyda.abit.core.model.Schedule
import com.gmail.volkovskiyda.abit.core.model.ScheduleId
import com.gmail.volkovskiyda.abit.feature.schedules.impl.SchedulesUiState
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * The popover's second pane. It renders at the popover's own width with no window of its own, which
 * is the point: "Schedules" grows the popover rather than opening a second surface.
 */
@OptIn(ExperimentalTestApi::class)
class SchedulesPaneTest {
    @Test
    fun `lists the schedules and offers the way back to Today`() =
        runComposeUiTest {
            setContent {
                AbitTheme(darkTheme = false) {
                    SchedulesPaneContent(
                        state = SchedulesUiState(schedules = listOf(schedule())),
                        onToggle = { _, _ -> },
                        onBack = {},
                    )
                }
            }

            onNodeWithText("Schedules").assertIsDisplayed()
            onNodeWithText("Deep work").assertIsDisplayed()
            onNodeWithText("‹ Today").assertIsDisplayed()
        }

    @Test
    fun `the back control returns to the Today pane rather than closing a window`() =
        runComposeUiTest {
            var back = false
            setContent {
                AbitTheme(darkTheme = false) {
                    SchedulesPaneContent(
                        state = SchedulesUiState(schedules = listOf(schedule())),
                        onToggle = { _, _ -> },
                        onBack = { back = true },
                    )
                }
            }

            onNodeWithText("‹ Today").performClick()

            assertTrue(back, "the pane offers no way back to Today")
        }

    @Test
    fun `says where a schedule comes from when there are none`() =
        runComposeUiTest {
            setContent {
                AbitTheme(darkTheme = false) {
                    SchedulesPaneContent(state = SchedulesUiState(), onToggle = { _, _ -> }, onBack = {})
                }
            }

            onNodeWithText("No schedules yet. Add one on your phone.").assertIsDisplayed()
        }

    private fun schedule() =
        Schedule(
            id = ScheduleId("one"),
            name = "Deep work",
            enabled = true,
            days = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY),
            start = LocalTime(9, 0),
            end = LocalTime(12, 0),
            focusMinutes = 45,
            breakMinutes = 15,
            updatedAt = Instant.fromEpochSeconds(0),
        )
}
