package com.gmail.volkovskiyda.abit.chime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gmail.volkovskiyda.abit.core.chime.ChimeCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * A reboot clears every alarm, and so does an app update — hence `MY_PACKAGE_REPLACED` beside
 * `BOOT_COMPLETED` in the manifest. Without this the app would go quiet until something else
 * happened to re-arm it, which on an ambient app means silently until the user next opened it.
 */
class BootReceiver :
    BroadcastReceiver(),
    KoinComponent {
    private val coordinator: ChimeCoordinator by inject()

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        // Both actions are protected broadcasts only the system can send, but a receiver that does
        // not check the action can still be woken by a spoofed intent with none.
        if (intent.action !in HANDLED_ACTIONS) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                coordinator.onChimeFired()
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val HANDLED_ACTIONS = setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED)
    }
}
