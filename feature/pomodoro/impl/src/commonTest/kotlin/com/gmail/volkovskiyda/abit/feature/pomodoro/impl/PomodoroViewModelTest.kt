package com.gmail.volkovskiyda.abit.feature.pomodoro.impl

import app.cash.turbine.test
import com.gmail.volkovskiyda.abit.core.domain.SyncState
import com.gmail.volkovskiyda.abit.core.model.PomodoroSession
import com.gmail.volkovskiyda.abit.core.testing.FakePomodoroSessionRepository
import com.gmail.volkovskiyda.abit.core.testing.FakeSyncStatusRepository
import com.gmail.volkovskiyda.abit.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class PomodoroViewModelTest {

    private val mainDispatcherRule = MainDispatcherRule()

    @BeforeTest
    fun setUp() = mainDispatcherRule.setUp()

    @AfterTest
    fun tearDown() = mainDispatcherRule.tearDown()

    @Test
    fun `emits stored sessions and the sync state`() = runTest {
        val session = PomodoroSession(
            id = "one",
            startedAt = Instant.fromEpochSeconds(1_700_000_000),
            durationMinutes = 25,
            completed = true,
            updatedAt = Instant.fromEpochSeconds(1_700_000_000),
        )
        val viewModel = PomodoroViewModel(
            sessionRepository = FakePomodoroSessionRepository(listOf(session)),
            syncStatusRepository = FakeSyncStatusRepository(),
        )

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(listOf(session), state.sessions)
            assertEquals(SyncState.Unavailable, state.syncState)
        }
    }
}
