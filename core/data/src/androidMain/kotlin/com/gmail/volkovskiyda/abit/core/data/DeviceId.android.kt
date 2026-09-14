package com.gmail.volkovskiyda.abit.core.data

import android.os.Build

actual fun deviceId(): String = "${Build.MANUFACTURER} ${Build.MODEL}"
