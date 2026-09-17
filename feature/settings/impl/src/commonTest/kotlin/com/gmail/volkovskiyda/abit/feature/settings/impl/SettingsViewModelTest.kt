package com.gmail.volkovskiyda.abit.feature.settings.impl

import androidx.datastore.core.DataStore
import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import com.gmail.volkovskiyda.abit.core.chime.ChimePreview
import com.gmail.volkovskiyda.abit.core.common.AppVersion
import com.gmail.volkovskiyda.abit.core.datastore.ChimeSound
import com.gmail.volkovskiyda.abit.core.datastore.ThemeMode
import com.gmail.volkovskiyda.abit.core.datastore.UserPreferences
import com.gmail.volkovskiyda.abit.core.datastore.UserPreferencesRepository
import com.gmail.volkovskiyda.abit.core.testing.FakeAuthRepository
import com.gmail.volkovskiyda.abit.core.testing.FakeLogger
import com.gmail.volkovskiyda.abit.core.testing.FakeSyncStatusRepository
import com.gmail.volkovskiyda.abit.core.testing.MainDispatcherRule
import com.gmail.volkovskiyda.abit.feature.settings.api.PermissionId
import com.gmail.volkovskiyda.abit.feature.settings.api.PermissionReader
import com.gmail.volkovskiyda.abit.feature.settings.api.PermissionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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

/**
 * Waits for the value the screen actually renders. The first one a collector sees is from before the
 * preferences file had been read, and `preferences are null until they have been read` is the test
 * that says so — everything else here is about a stored value and starts from the read.
 */
private suspend fun TurbineTestContext<SettingsUiState>.awaitLoaded(): SettingsUiState {
    var item = awaitItem()
    while (item.preferences == null) item = awaitItem()
    return item
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
                awaitLoaded()

                viewModel.setChimeSound(ChimeSound.SoftBell)
                assertEquals(ChimeSound.SoftBell, awaitItem().preferences?.chimeSound)

                viewModel.setShowCountdownNotification(true)
                assertEquals(true, awaitItem().preferences?.showCountdownNotification)

                viewModel.setThemeMode(ThemeMode.Dark)
                assertEquals(ThemeMode.Dark, awaitItem().preferences?.themeMode)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `permission states come from the platform and can change while backgrounded`() =
        runTest {
            val denied = listOf(PermissionState(PermissionId.Notifications, granted = false))
            val viewModel = viewModel(permissions = denied)

            viewModel.state.test {
                assertEquals(denied, awaitLoaded().permissions)

                viewModel.onPermissionsChanged(listOf(PermissionState(PermissionId.Notifications, granted = true)))

                assertEquals(
                    listOf(PermissionState(PermissionId.Notifications, granted = true)),
                    awaitItem().permissions,
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `the platform's permissions are in the very first value, before anything is collected`() =
        runTest {
            // The settings screen is rebuilt on every visit and renders this value. A list that
            // starts empty is read as "denied" by every row that looks itself up in it, so the rows
            // drew an Allow button and then took it away again.
            val granted = listOf(PermissionState(PermissionId.ExactAlarms, granted = true))

            assertEquals(granted, viewModel(permissions = granted).state.value.permissions)
        }

    @Test
    fun `preferences are null until they have been read, never the defaults`() =
        runTest {
            // Null rather than `UserPreferences()`: the defaults are a real answer for a user who
            // has never changed anything, so a screen cannot tell "off" from "not read yet" unless
            // the state says so — and it drew the defaults, then visibly corrected itself.
            val viewModel = viewModel()

            assertNull(viewModel.state.value.preferences)

            viewModel.state.test {
                assertNotNull(awaitLoaded().preferences)
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
                awaitLoaded()

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
                assertEquals(false, awaitLoaded().preferences?.showCountdownNotification)

                viewModel.setShowCountdownNotification(true)

                assertEquals(true, awaitItem().preferences?.showCountdownNotification)
                assertEquals(1, preview.countdowns)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `the running build is in the state from the first emission`() =
        runTest {
            // It has to be there before the first flow arrives: the settings screen renders the
            // initial value, and a version that appears a frame later reads as a glitch.
            val viewModel = viewModel()

            assertEquals("1.2.345", viewModel.state.value.appVersion)

            viewModel.state.test {
                assertEquals("1.2.345", awaitLoaded().appVersion)
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun TestScope.viewModel(
        chimePreview: ChimePreview = RecordingChimePreview(),
        permissions: List<PermissionState> = emptyList(),
    ) = SettingsViewModel(
        preferencesRepository = UserPreferencesRepository(InMemoryPreferences(), backgroundScope, FakeLogger()),
        authRepository = FakeAuthRepository(),
        chimePreview = chimePreview,
        syncStatusRepository = FakeSyncStatusRepository(),
        appVersion = AppVersion("1.2.345"),
        permissionReader = { permissions },
    )
}
