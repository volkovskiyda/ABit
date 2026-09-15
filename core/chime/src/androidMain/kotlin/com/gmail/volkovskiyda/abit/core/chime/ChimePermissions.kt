package com.gmail.volkovskiyda.abit.core.chime

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService

/**
 * The two things Android can silently withhold that make this app pointless: permission to post a
 * notification, and permission to wake at an exact minute.
 *
 * Both are read here rather than in a screen so the phone and the watch answer the same way, and so
 * a caller that forgets the API-level guard cannot exist — `canScheduleExactAlarms()` only appears
 * from API 31 and `minSdk` is 30.
 */
class ChimePermissions(
    private val context: Context,
) {
    fun canScheduleExactAlarms(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarms = context.getSystemService<AlarmManager>() ?: return false
        return alarms.canScheduleExactAlarms()
    }

    fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    /** The system screen that grants exact alarms. There is no runtime prompt for this one. */
    fun exactAlarmSettingsIntent(): Intent =
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:${context.packageName}".toUriOrNull())

    /** The app's notification settings, for when the runtime prompt has already been refused. */
    fun notificationSettingsIntent(): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

    private fun String.toUriOrNull(): Uri? = runCatching { Uri.parse(this) }.getOrNull()
}
