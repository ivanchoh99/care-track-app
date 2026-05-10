package com.app.caretrack.common

import android.util.Log

actual object AppLogger {
    actual fun e(tag: String, message: String) { Log.e(tag, message) }
    actual fun w(tag: String, message: String) { Log.w(tag, message) }
    actual fun d(tag: String, message: String) { Log.d(tag, message) }
}
