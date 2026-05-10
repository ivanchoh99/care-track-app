package com.app.caretrack.common

import Foundation

actual object AppLogger {
    actual fun e(tag: String, message: String) { NSLog("ERROR/%@: %@", tag, message) }
    actual fun w(tag: String, message: String) { NSLog("WARN/%@: %@", tag, message) }
    actual fun d(tag: String, message: String) { NSLog("DEBUG/%@: %@", tag, message) }
}
