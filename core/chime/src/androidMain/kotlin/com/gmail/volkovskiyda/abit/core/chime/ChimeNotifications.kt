package com.gmail.volkovskiyda.abit.core.chime

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import com.gmail.volkovskiyda.abit.core.domain.BlockKind
import com.gmail.volkovskiyda.abit.core.domain.ChimeKind
import com.gmail.volkovskiyda.abit.core.domain.TodayState
import kotlinx.datetime.LocalTime

internal const val CHANNEL_CHIMES = "abit.chimes"
internal const val CHANNEL_COUNTDOWN = "abit.countdown"

internal const val NOTIFICATION_CHIME = 1001
internal const val NOTIFICATION_COUNTDOWN = 1002

/**
 * Two channels, deliberately different: the boundary itself is a high-importance alert with the
 * platform's own sound, while the countdown is a silent ongoing notification the *system* ticks.
 *
 * The countdown uses `setUsesChronometer` + `setChronometerCountDown` + `setWhen(sessionEnd)` rather
 * than a repost per second. That is what makes an ongoing countdown affordable without a foreground
 * service: the app posts once per boundary and Android counts down on its own.
 */
class ChimeNotifications(
    private val context: Context,
    private val permissions: ChimePermissions,
) {
    private val manager = NotificationManagerCompat.from(context)

    private val isWatch: Boolean
        get() = context.packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH)

    fun postChime(
        kind: ChimeKind,
        scheduleName: String,
        focusEnd: LocalTime,
        sessionEnd: LocalTime,
        vibrate: Boolean,
    ) {
        if (!permissions.canPostNotifications()) return
        ensureChannels(vibrate)

        val title =
            when (kind) {
                ChimeKind.FocusStart -> "Focus"
                ChimeKind.BreakStart -> "Break"
                ChimeKind.DayEnd -> "Done for today"
            }
        val caption =
            when (kind) {
                ChimeKind.FocusStart -> "Focus until ${focusEnd.hhmm()} · $scheduleName"
                ChimeKind.BreakStart -> "Break until ${sessionEnd.hhmm()} · $scheduleName"
                ChimeKind.DayEnd -> "That is the last block of $scheduleName"
            }
        val notification =
            NotificationCompat
                .Builder(context, CHANNEL_CHIMES)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText(caption)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
        manager.notify(NOTIFICATION_CHIME, notification)
    }

    /** Posts or clears the ongoing countdown. Only a [TodayState.Running] day has one. */
    fun showCountdown(
        state: TodayState,
        enabled: Boolean,
    ) {
        if (!enabled || state !is TodayState.Running || !permissions.canPostNotifications()) {
            manager.cancel(NOTIFICATION_COUNTDOWN)
            return
        }
        ensureChannels(vibrate = false)

        val stage = if (state.stage == BlockKind.Focus) "Focus" else "Break"
        val notification =
            NotificationCompat
                .Builder(context, CHANNEL_COUNTDOWN)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("$stage · session ${state.sessionNumber} of ${state.sessionCount}")
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setShowWhen(true)
                // The system ticks this, so the app posts once per boundary rather than once a second.
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(System.currentTimeMillis() + state.stageRemaining.inWholeMilliseconds)
                .build()
        manager.notify(NOTIFICATION_COUNTDOWN, notification)
    }

    fun clearCountdown() {
        manager.cancel(NOTIFICATION_COUNTDOWN)
    }

    private fun ensureChannels(vibrate: Boolean) {
        val system = context.getSystemService<NotificationManager>() ?: return
        system.createNotificationChannel(
            NotificationChannel(CHANNEL_CHIMES, "Chimes", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "The sound at every focus and break boundary."
                enableVibration(vibrate)
                // No setSound call on purpose: a channel left alone uses the platform's default
                // notification sound, which is decision 12A. A synthesized bell here would override
                // the one the user chose for their phone.
                if (isWatch) setShowBadge(false)
            },
        )
        system.createNotificationChannel(
            NotificationChannel(CHANNEL_COUNTDOWN, "Countdown", NotificationManager.IMPORTANCE_LOW).apply {
                description = "The quiet ongoing countdown to the end of the current session."
                setShowBadge(false)
                enableVibration(false)
            },
        )
    }
}

private fun LocalTime.hhmm(): String = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
