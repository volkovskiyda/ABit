package com.gmail.volkovskiyda.abit.feature.pomodoro.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * How anything reaches the pomodoro screen. It lives in `:api` rather than `:impl` so another
 * feature can navigate here without compiling against this feature's ViewModels — that separation
 * is the whole point of the api/impl split.
 */
@Serializable
data object PomodoroNavKey : NavKey
