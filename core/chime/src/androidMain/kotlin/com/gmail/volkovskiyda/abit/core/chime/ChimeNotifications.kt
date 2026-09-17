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

/**
 * The sample's own channel, because it is the one countdown notification that has to announce
 * itself. It is posted from Settings, where the shade is closed, and a silent sample is
 * indistinguishable from a switch that did nothing.
 */
internal const val CHANNEL_COUNTDOWN_SAMPLE = "abit.countdown.sample"

internal const val NOTIFICATION_CHIME = 1001
internal const val NOTIFICATION_COUNTDOWN = 1002

/** Its own id, so a sample never stands in for the real countdown nor is cancelled along with it. */
internal const val NOTIFICATION_SAMPLE_COUNTDOWN = 1003

/**
 * How long a boundary notification stays before the system takes it away.
 *
 * The chime is the point; the card is only what carries the platform's sound, and a "Break · until
 * 14:30" left in the shade an hour later says nothing true. Ten seconds outlasts the heads-up peek —
 * which is about five — so the banner is never pulled out from under someone reading it, and the
 * shade is clean by the time anyone opens it. The *system* removes it, so it needs neither a
 * coroutine nor the process to still be alive at the deadline.
 */
private const val CHIME_TIMEOUT_MILLIS = 10_000L

/** What the sample counts down from — a plausible remainder rather than a round, obviously fake one. */
private const val SAMPLE_REMAINING_MILLIS = 24L * 60L * 1000L

/** Long enough to read, short enough that nobody is left with a second countdown to dismiss. */
private const val SAMPLE_TIMEOUT_MILLIS = 12_000L

/**
 * Three channels, deliberately different: the boundary itself is a high-importance alert with the
 * platform's own sound, the countdown is a silent ongoing notification the *system* ticks, and the
 * countdown's sample is high-importance too, so that it reaches the user who just asked to see it.
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
    ) {
        if (!permissions.canPostNotifications()) return
        ensureChannels()

        val title =
            when (kind) {
                ChimeKind.FocusStart -> "Focus"
                ChimeKind.BreakStart -> "Break"
                ChimeKind.DayEnd -> "Done for today"
            }
        val caption =
            when (kind) {
                ChimeKind.FocusStart -> "until ${focusEnd.hhmm()}"
                ChimeKind.BreakStart -> "until ${sessionEnd.hhmm()}"
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
                .setTimeoutAfter(CHIME_TIMEOUT_MILLIS)
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
        ensureChannels()

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

    /**
     * One dismissible copy of the countdown, for the switch in Settings that turns it on.
     *
     * The real one exists only while a session is running, which is rarely when someone is in
     * Settings deciding whether they want it. This is labelled as a sample and given a timeout, so
     * the user gets a look at the shape of the thing rather than a second countdown to live with.
     *
     * Unlike the real countdown it is neither ongoing nor silent: it peeks over Settings with the
     * platform's notification sound, because the user is looking at the switch they just flipped
     * and not at the shade.
     */
    fun postSampleCountdown() {
        if (!permissions.canPostNotifications()) return
        ensureChannels()

        val notification =
            NotificationCompat
                .Builder(context, CHANNEL_COUNTDOWN_SAMPLE)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Focus · session 3 of 9")
                .setContentText("Sample · this is how a running session looks")
                .setAutoCancel(true)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(System.currentTimeMillis() + SAMPLE_REMAINING_MILLIS)
                // The system takes it away, so it survives the user leaving Settings and does not
                // depend on a coroutine that the screen going away would cancel.
                .setTimeoutAfter(SAMPLE_TIMEOUT_MILLIS)
                .build()
        manager.notify(NOTIFICATION_SAMPLE_COUNTDOWN, notification)
    }

    private fun ensureChannels() {
        val system = context.getSystemService<NotificationManager>() ?: return
        system.createNotificationChannel(
            NotificationChannel(CHANNEL_CHIMES, "Chimes", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "The sound at every focus and break boundary."
                // No setSound call on purpose: a channel left alone uses the platform's default
                // notification sound, which is decision 12A. A synthesized bell here would override
                // the one the user chose for their phone. What this must not do is leave the
                // channel silent — a boundary nobody hears is the whole feature missing — so the
                // importance stays HIGH and the sound stays the platform's.
                //
                // Vibration on, with no in-app switch. A channel's vibration can only be set as the
                // channel is created: createNotificationChannel ignores it, and the importance,
                // for an id that already exists. That is exactly why the switch which used to pass
                // a flag here did nothing from the second chime onwards, and why this belongs to
                // the system's own channel settings, which were always the half that worked.
                enableVibration(true)
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
        system.createNotificationChannel(
            // High, not low: importance is the only thing that decides whether a notification peeks
            // on this minSdk, and a sample that waits in the shade is a sample the user never sees.
            // That is why it is a channel of its own rather than a flag on the builder — raising the
            // real countdown's importance would make every boundary of every session noisy.
            NotificationChannel(
                CHANNEL_COUNTDOWN_SAMPLE,
                "Countdown sample",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "The one-off preview shown when you turn the countdown on."
                setShowBadge(false)
                enableVibration(false)
            },
        )
    }
}

private fun LocalTime.hhmm(): String = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
