package com.gmail.volkovskiyda.abit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.gmail.volkovskiyda.abit.feature.pomodoro.api.PomodoroNavKey
import com.gmail.volkovskiyda.abit.ui.PomodoroScreen
import com.gmail.volkovskiyda.abit.ui.theme.AbitTheme
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            AbitTheme {
                AbitNavDisplay()
            }
        }
    }
}

/**
 * One entry today. The `entryProvider { entry<K> { } }` DSL is deliberate: it is the shape the
 * Kotzilla compiler plugin rewrites to record screen views, so a screen is registered by adding a
 * key here rather than by hand-wrapping each composable.
 */
@Composable
private fun AbitNavDisplay() {
    val backStack = rememberNavBackStack(PomodoroNavKey)
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider<NavKey> {
            entry<PomodoroNavKey> { PomodoroScreen(viewModel = koinViewModel()) }
        },
    )
}
