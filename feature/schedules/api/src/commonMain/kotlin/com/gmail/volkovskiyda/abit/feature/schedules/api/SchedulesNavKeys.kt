package com.gmail.volkovskiyda.abit.feature.schedules.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object SchedulesNavKey : NavKey

/** [id] is null for a new schedule. */
@Serializable
data class ScheduleEditorNavKey(
    val id: String? = null,
) : NavKey

/**
 * The sheet that asks which of two overlapping schedules stays on. It carries both ids rather than
 * the whole conflict: a nav key is serialized into back-stack state, and the conflict is re-derived
 * from the schedules anyway.
 */
@Serializable
data class ScheduleConflictNavKey(
    val first: String,
    val second: String,
) : NavKey
