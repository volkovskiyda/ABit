package com.gmail.volkovskiyda.abit.feature.settings.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object SettingsNavKey : NavKey

/**
 * The sign-in sheet. A destination rather than a dialog because it is reachable from two places —
 * the sync badge on Today and the card at the top of Settings — and from neither is it a launch wall.
 */
@Serializable
data object SignInNavKey : NavKey
