package com.app.caretrack

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform