package com.gmail.volkovskiyda.abit.feature.settings.api

/**
 * A permission this app needs in order to do the only thing it exists for. Which ones are real
 * depends on the platform, so the feature holds the identifiers and each app renders only the rows
 * that mean something for it.
 */
enum class PermissionId {
    /** Android and Wear: `POST_NOTIFICATIONS`. */
    Notifications,

    /** Android and Wear: `SCHEDULE_EXACT_ALARM`. Refusing it costs punctuality, not the chime. */
    ExactAlarms,

    /** Web: the browser's own notification permission. */
    BrowserNotifications,
}

data class PermissionState(
    val id: PermissionId,
    val granted: Boolean,
)
