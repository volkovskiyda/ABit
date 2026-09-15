package com.gmail.volkovskiyda.abit.app.shared

import android.content.Context
import kotlin.reflect.KClass

internal actual val koinVerifyExtraTypes: List<KClass<*>> = listOf(Context::class)
