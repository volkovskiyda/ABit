package com.gmail.volkovskiyda.abit.feature.today.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * The app's home: what mode it is now, when the next chime is, and what the rest of today looks like.
 * In `:api` so another feature can navigate here without compiling against this one's ViewModel.
 */
@Serializable
data object TodayNavKey : NavKey
