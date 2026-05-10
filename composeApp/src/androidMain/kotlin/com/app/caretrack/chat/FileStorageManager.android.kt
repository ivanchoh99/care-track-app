package com.app.caretrack.chat

import android.content.Context
import java.io.File

actual class FileStorageManager actual constructor(context: Any?) {
    private val appContext = (context as? Context)?.applicationContext

    actual fun saveFile(bytes: ByteArray, fileName: String): String {
        val dir = File(appContext?.filesDir, "media")
        dir.mkdirs()
        val file = File(dir, fileName)
        file.writeBytes(bytes)
        return file.absolutePath
    }
}
