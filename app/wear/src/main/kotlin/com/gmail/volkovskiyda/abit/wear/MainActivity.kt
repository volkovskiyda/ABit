package com.gmail.volkovskiyda.abit.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.wear.tooling.preview.devices.WearDevices
import com.gmail.volkovskiyda.abit.core.chime.ChimePermissions
import com.gmail.volkovskiyda.abit.core.domain.BlockKind
import com.gmail.volkovskiyda.abit.feature.schedules.impl.SchedulesUiState
import com.gmail.volkovskiyda.abit.feature.settings.api.PermissionId
import com.gmail.volkovskiyda.abit.feature.settings.api.PermissionState
import com.gmail.volkovskiyda.abit.feature.today.impl.TodayUiState
import com.gmail.volkovskiyda.abit.feature.today.impl.TodayViewModel
import com.gmail.volkovskiyda.abit.wear.auth.GoogleSignIn
import com.gmail.volkovskiyda.abit.wear.ui.OnWearResume
import com.gmail.volkovskiyda.abit.wear.ui.WearChimeScreen
import com.gmail.volkovskiyda.abit.wear.ui.WearSchedulesContent
import com.gmail.volkovskiyda.abit.wear.ui.WearSchedulesScreen
import com.gmail.volkovskiyda.abit.wear.ui.WearSignInState
import com.gmail.volkovskiyda.abit.wear.ui.WearTodayContent
import com.gmail.volkovskiyda.abit.wear.ui.WearTodayScreen
import com.gmail.volkovskiyda.abit.wear.ui.theme.AbitWearTheme
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import androidx.compose.ui.tooling.preview.Preview as ComposePreview

private const val ROUTE_TODAY = "today"
private const val ROUTE_SCHEDULES = "schedules"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AbitWearTheme {
                AppScaffold {
                    AbitWearApp()
                }
            }
        }
    }
}

/**
 * `SwipeDismissableNavHost` rather than Navigation 3: Wear's idiom is swipe-to-dismiss, and the
 * watch has two destinations plus a chime screen the notification launches. The feature api's nav
 * keys still say *which* screens exist; the host is Wear's own.
 */
@Composable
internal fun AbitWearApp() {
    val controller = rememberSwipeDismissableNavController()
    val permissions: ChimePermissions = koinInject()
    val context = LocalContext.current
    var permissionStates by remember { mutableStateOf(emptyList<PermissionState>()) }

    OnWearResume {
        permissionStates =
            listOf(
                PermissionState(PermissionId.Notifications, permissions.canPostNotifications()),
                PermissionState(PermissionId.ExactAlarms, permissions.canScheduleExactAlarms()),
            )
    }

    SwipeDismissableNavHost(navController = controller, startDestination = ROUTE_TODAY) {
        composable(ROUTE_TODAY) {
            WearTodayScreen(viewModel = koinViewModel())
        }
        composable(ROUTE_SCHEDULES) {
            val todayViewModel: TodayViewModel = koinViewModel()
            val today by todayViewModel.state.collectAsStateWithLifecycle()
            val googleSignIn = remember(context) { GoogleSignIn(context) }
            val scope = rememberCoroutineScope()

            WearSchedulesScreen(
                viewModel = koinViewModel(),
                permissions = permissionStates,
                signIn =
                    WearSignInState(
                        needsSignIn = today.user?.isAnonymous != false,
                        available = googleSignIn.serverClientId != null,
                        error = today.authError,
                        onSignIn = {
                            val clientId = googleSignIn.serverClientId ?: return@WearSignInState
                            scope.launch {
                                googleSignIn
                                    .requestIdToken(clientId)
                                    .onSuccess(todayViewModel::signInWithGoogle)
                                    .onFailure { todayViewModel.onAuthError(it.message ?: "Sign-in failed") }
                            }
                        },
                    ),
                onFixPermission = { state ->
                    val intent =
                        when (state.id) {
                            PermissionId.ExactAlarms -> permissions.exactAlarmSettingsIntent()
                            else -> permissions.notificationSettingsIntent()
                        }
                    context.startActivity(intent)
                },
            )
        }
    }
}

@ComposePreview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Composable
private fun WearTodayLargeRoundPreview() {
    AbitWearTheme { AppScaffold { WearTodayContent(TodayUiState(), onSkipToday = {}) } }
}

@ComposePreview(device = WearDevices.SQUARE, showSystemUi = true)
@Composable
private fun WearTodaySquarePreview() {
    AbitWearTheme { AppScaffold { WearTodayContent(TodayUiState(), onSkipToday = {}) } }
}

@ComposePreview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Composable
private fun WearSchedulesLargeRoundPreview() {
    AbitWearTheme {
        AppScaffold {
            WearSchedulesContent(
                state = SchedulesUiState(),
                missingPermissions = emptyList(),
                onToggle = { _, _ -> },
                onFixPermission = {},
            )
        }
    }
}

@ComposePreview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Composable
private fun WearChimeLargeRoundPreview() {
    AbitWearTheme {
        WearChimeScreen(
            stage = BlockKind.Break,
            untilLabel = "until 10:00",
            thenLabel = "then focus for 45 min",
            onDismiss = {},
        )
    }
}
