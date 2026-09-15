package com.gmail.volkovskiyda.abit.core.chime

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import com.gmail.volkovskiyda.abit.core.common.TimeZoneProvider
import com.gmail.volkovskiyda.abit.core.datastore.UserPreferencesRepository
import com.gmail.volkovskiyda.abit.core.domain.Chime
import com.gmail.volkovskiyda.abit.core.domain.TodayState
import kotlinx.coroutines.flow.first
import kotlinx.datetime.toInstant

/**
 * The intent action the receiver filters on. Package-qualified because an implicit action name is a
 * way for another app to wake this one.
 */
const val ACTION_CHIME = "com.gmail.volkovskiyda.abit.action.CHIME"

/**
 * What the pending intent carries, so the receiver can post the right words without re-deriving the
 * plan at a moment when the database may not have caught up with the edit that moved the boundary.
 */
const val EXTRA_KIND = "abit.kind"
const val EXTRA_SCHEDULE_NAME = "abit.scheduleName"
const val EXTRA_FOCUS_END_SECOND = "abit.focusEndSecond"
const val EXTRA_SESSION_END_SECOND = "abit.sessionEndSecond"

/**
 * One fixed request code, so arming replaces the pending alarm rather than adding another. The whole
 * scheduler holds exactly one armed chime at a time, which is what `ChimeCoordinator` assumes.
 */
private const val CHIME_REQUEST_CODE = 1

/**
 * Wakes the app at a boundary through `AlarmManager`.
 *
 * Exact where it is allowed and inexact where it is not (decision 9A): `SCHEDULE_EXACT_ALARM` is a
 * permission the user can refuse, and refusing it must degrade the chime's punctuality rather than
 * remove it. `canScheduleExactAlarms()` exists only from API 31 while `minSdk` is 30, so the check
 * lives in [ChimePermissions] rather than being repeated here.
 *
 * `core:chime`'s Android source set is shared by the phone and the watch, so this one class serves
 * both apps; only the notification differs, and [ChimeNotifications] handles that.
 */
class AndroidChimeScheduler(
    private val context: Context,
    private val notifications: ChimeNotifications,
    private val permissions: ChimePermissions,
    private val preferences: UserPreferencesRepository,
    private val timeZoneProvider: TimeZoneProvider,
    private val surfaces: ChimeSurfaceUpdater,
) : ChimeScheduler {
    private val alarms: AlarmManager?
        get() = context.getSystemService()

    override suspend fun arm(chime: Chime) {
        val manager = alarms ?: return
        val triggerAtMillis = chime.at.toInstant(timeZoneProvider.current()).toEpochMilliseconds()
        val pending = chimeIntent(chime)

        if (permissions.canScheduleExactAlarms()) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
        } else {
            // Doze may delay this by minutes. Better late than never: the alternative is silence.
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
        }
        surfaces.onArmedChimeChanged()
    }

    override suspend fun disarm() {
        // `Intent.filterEquals` ignores extras, so a bare intent still matches what was armed.
        val pending = chimeIntent(chime = null)
        alarms?.cancel(pending)
        // AlarmManager.cancel drops the alarm but leaves the PendingIntent alive; cancelling it too
        // is what makes "nothing is armed" observably true rather than merely effectively true.
        pending.cancel()
        notifications.clearCountdown()
        surfaces.onArmedChimeChanged()
    }

    override suspend fun showCountdown(state: TodayState) {
        val prefs = preferences.preferences.first()
        notifications.showCountdown(
            state = state,
            enabled = prefs.showCountdownNotification && prefs.chimeOnThisDevice,
        )
    }

    /**
     * Resolved rather than named: this module is a library and cannot import either app's receiver
     * class, and an *implicit* broadcast would not reach a manifest-declared receiver on Android 8+
     * anyway. The manifest filter exists so this lookup can find it.
     */
    private fun chimeComponent(): ComponentName? =
        context.packageManager
            .queryBroadcastReceivers(Intent(ACTION_CHIME).setPackage(context.packageName), 0)
            .firstOrNull()
            ?.activityInfo
            ?.let { ComponentName(it.packageName, it.name) }

    private fun chimeIntent(chime: Chime?): PendingIntent {
        val intent = Intent(ACTION_CHIME).setPackage(context.packageName)
        chimeComponent()?.let(intent::setComponent)
        if (chime != null) {
            intent
                .putExtra(EXTRA_KIND, chime.kind.name)
                .putExtra(EXTRA_SCHEDULE_NAME, chime.scheduleName)
                .putExtra(
                    EXTRA_FOCUS_END_SECOND,
                    chime.session.focus.end
                        .toSecondOfDay(),
                ).putExtra(EXTRA_SESSION_END_SECOND, chime.session.end.toSecondOfDay())
        }
        return PendingIntent.getBroadcast(
            context,
            CHIME_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
