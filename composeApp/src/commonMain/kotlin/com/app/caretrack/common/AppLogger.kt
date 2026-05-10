package com.app.caretrack.common

expect object AppLogger {
    fun e(tag: String, message: String)
    fun w(tag: String, message: String)
    fun d(tag: String, message: String)
}
