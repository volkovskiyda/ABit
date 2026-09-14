package com.gmail.volkovskiyda.abit

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform