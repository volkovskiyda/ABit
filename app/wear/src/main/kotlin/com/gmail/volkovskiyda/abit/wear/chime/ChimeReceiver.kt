package com.gmail.volkovskiyda.abit.wear.chime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gmail.volkovskiyda.abit.core.chime.ChimeCoordinator
import com.gmail.volkovskiyda.abit.core.chime.ChimeNotifications
import com.gmail.volkovskiyda.abit.core.chime.EXTRA_FOCUS_END_SECOND
import com.gmail.volkovskiyda.abit.core.chime.EXTRA_KIND
import com.gmail.volkovskiyda.abit.core.chime.EXTRA_SCHEDULE_NAME
import com.gmail.volkovskiyda.abit.core.chime.EXTRA_SESSION_END_SECOND
import com.gmail.volkovskiyda.abit.core.datastore.UserPreferencesRepository
import com.gmail.volkovskiyda.abit.core.domain.ChimeKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * What `AlarmManager` wakes. It posts the boundary's notification, then asks the coordinator to arm
 * the next one — the coordinator is the only thing that knows what "next" is.
 *
 * `goAsync()` because both halves suspend: a `BroadcastReceiver` that returns from `onReceive`
 * before its coroutine finishes is a process Android is free to kill mid-write.
 */
class ChimeReceiver :
    BroadcastReceiver(),
    KoinComponent {
    private val coordinator: ChimeCoordinator by inject()
    private val notifications: ChimeNotifications by inject()
    private val preferences: UserPreferencesRepository by inject()

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val kind = intent.getStringExtra(EXTRA_KIND)?.let { runCatching { ChimeKind.valueOf(it) }.getOrNull() }
                if (kind != null && preferences.preferences.first().chimeOnThisDevice) {
                    notifications.postChime(
                        kind = kind,
                        scheduleName = intent.getStringExtra(EXTRA_SCHEDULE_NAME).orEmpty(),
                        focusEnd = LocalTime.fromSecondOfDay(intent.getIntExtra(EXTRA_FOCUS_END_SECOND, 0)),
                        sessionEnd = LocalTime.fromSecondOfDay(intent.getIntExtra(EXTRA_SESSION_END_SECOND, 0)),
                        vibrate = preferences.preferences.first().vibrate,
                    )
                }
                coordinator.onChimeFired()
            } finally {
                pending.finish()
            }
        }
    }
}
