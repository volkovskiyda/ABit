package com.gmail.volkovskiyda.abit.feature.settings.impl

import androidx.datastore.core.DataStore
import app.cash.turbine.test
import com.gmail.volkovskiyda.abit.core.chime.ChimePreview
import com.gmail.volkovskiyda.abit.core.datastore.ChimeSound
import com.gmail.volkovskiyda.abit.core.datastore.ThemeMode
import com.gmail.volkovskiyda.abit.core.datastore.UserPreferences
import com.gmail.volkovskiyda.abit.core.datastore.UserPreferencesRepository
import com.gmail.volkovskiyda.abit.core.testing.FakeAuthRepository
import com.gmail.volkovskiyda.abit.core.testing.FakeSyncStatusRepository
import com.gmail.volkovskiyda.abit.core.testing.MainDispatcherRule
import com.gmail.volkovskiyda.abit.feature.settings.api.PermissionId
import com.gmail.volkovskiyda.abit.feature.settings.api.PermissionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Records what the screen asked to be played, which is the only thing worth asserting on. */
private class RecordingChimePreview : ChimePreview {
    val chimes = mutableListOf<ChimeSound>()
    var countdowns = 0

    override suspend fun chime(sound: ChimeSound) {
        chimes += sound
    }

    override suspend fun countdown() {
        countdowns++
    }
}

/** A DataStore that is a `MutableStateFlow`, which is all [UserPreferencesRepository] needs. */
private class InMemoryPreferences : DataStore<UserPreferences> {
    private val state = MutableStateFlow(UserPreferences())

    override val data: Flow<UserPreferences> = state.asStateFlow()

    override suspend fun updateData(transform: suspend (UserPreferences) -> UserPreferences): UserPreferences =
        transform(state.value).also { state.value = it }
}

class SettingsViewModelTest {
    private val mainDispatcherRule = MainDispatcherRule()

    @BeforeTest
    fun setUp() = mainDispatcherRule.setUp()

    @AfterTest
    fun tearDown() = mainDispatcherRule.tearDown()

    @Test
    fun `the other per-device settings persist too`() =
        runTest {
            val viewModel = viewModel()

            viewModel.state.test {
                awaitItem()

                viewModel.setChimeSound(ChimeSound.SoftBell)
                assertEquals(ChimeSound.SoftBell, awaitItem().preferences.chimeSound)

                viewModel.setShowCountdownNotification(true)
                assertEquals(true, awaitItem().preferences.showCountdownNotification)

                viewModel.setThemeMode(ThemeMode.Dark)
                assertEquals(ThemeMode.Dark, awaitItem().preferences.themeMode)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `permission states come from the platform and can change while backgrounded`() =
        runTest {
            val viewModel = viewModel()

            viewModel.state.test {
                assertEquals(emptyList(), awaitItem().permissions)

                viewModel.onPermissionsChanged(listOf(PermissionState(PermissionId.Notifications, granted = true)))

                assertEquals(
                    listOf(PermissionState(PermissionId.Notifications, granted = true)),
                    awaitItem().permissions,
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `picking a sound plays it`() =
        runTest {
            // The only way to hear a chime without waiting for a boundary, now that the switch which
            // used to preview one is gone. On the web it is also the user gesture the browser wants
            // before it will play anything at all.
            val preview = RecordingChimePreview()
            val viewModel = viewModel(preview)

            viewModel.state.test {
                awaitItem()

                viewModel.setChimeSound(ChimeSound.SoftBell)
                awaitItem()

                assertEquals(listOf(ChimeSound.SoftBell), preview.chimes)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `the countdown is off until it is asked for, and shows a sample when it is`() =
        runTest {
            val preview = RecordingChimePreview()
            val viewModel = viewModel(preview)

            viewModel.state.test {
                assertEquals(false, awaitItem().preferences.showCountdownNotification)

                viewModel.setShowCountdownNotification(true)

                assertEquals(true, awaitItem().preferences.showCountdownNotification)
                assertEquals(1, preview.countdowns)
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun viewModel(chimePreview: ChimePreview = RecordingChimePreview()) =
        SettingsViewModel(
            preferencesRepository = UserPreferencesRepository(InMemoryPreferences()),
            authRepository = FakeAuthRepository(),
            chimePreview = chimePreview,
            syncStatusRepository = FakeSyncStatusRepository(),
        )
}
