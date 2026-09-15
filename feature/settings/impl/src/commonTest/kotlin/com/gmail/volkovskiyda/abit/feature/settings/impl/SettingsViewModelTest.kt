package com.gmail.volkovskiyda.abit.feature.settings.impl

import androidx.datastore.core.DataStore
import app.cash.turbine.test
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
    fun `every device chimes until this one is told not to`() =
        runTest {
            val viewModel = viewModel()

            viewModel.state.test {
                assertTrue(awaitItem().preferences.chimeOnThisDevice)

                viewModel.setChimeOnThisDevice(false)

                assertEquals(false, awaitItem().preferences.chimeOnThisDevice)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `the other per-device settings persist too`() =
        runTest {
            val viewModel = viewModel()

            viewModel.state.test {
                awaitItem()

                viewModel.setChimeSound(ChimeSound.SoftBell)
                assertEquals(ChimeSound.SoftBell, awaitItem().preferences.chimeSound)

                viewModel.setVibrate(false)
                assertEquals(false, awaitItem().preferences.vibrate)

                viewModel.setShowCountdownNotification(false)
                assertEquals(false, awaitItem().preferences.showCountdownNotification)

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

    private fun viewModel() =
        SettingsViewModel(
            preferencesRepository = UserPreferencesRepository(InMemoryPreferences()),
            authRepository = FakeAuthRepository(),
            syncStatusRepository = FakeSyncStatusRepository(),
        )
}
