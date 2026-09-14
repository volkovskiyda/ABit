package com.gmail.volkovskiyda.abit.core.data

actual fun deviceId(): String = System.getProperty("os.name").orEmpty().ifEmpty { "desktop" }
