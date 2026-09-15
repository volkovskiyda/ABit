package com.gmail.volkovskiyda.abit.core.model

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/** The signed-in user, or the anonymous one. Firestore documents live under `users/{id}`. */
@JvmInline
@Serializable
value class UserId(
    val value: String,
)
