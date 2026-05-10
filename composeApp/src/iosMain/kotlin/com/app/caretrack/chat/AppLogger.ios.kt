package com.app.caretrack.chat

actual object AppLogger {
    actual fun e(tag: String, message: String) = println("ERROR/$tag: $message")
    actual fun w(tag: String, message: String) = println("WARN/$tag: $message")
    actual fun d(tag: String, message: String) = println("DEBUG/$tag: $message")
}
