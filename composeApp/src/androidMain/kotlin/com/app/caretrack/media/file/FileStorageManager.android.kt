package com.app.caretrack.media.file

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.app.caretrack.common.AppLogger
import java.io.File

actual class FileStorageManager actual constructor(context: Any?) {
    private val appContext = (context as? Context)?.applicationContext

    actual fun saveFile(bytes: ByteArray, fileName: String): String {
        val dir = File(appContext?.filesDir, "media")
        if (!dir.exists()) {
            dir.mkdirs()
        }

        val file = File(dir, fileName)
        file.writeBytes(bytes)
        return file.absolutePath
    }

    actual fun deleteFile(path: String): Boolean {
        return try {
            File(path).delete()
        } catch (e: Exception) {
            AppLogger.e("FileStorage", "Error deleting file: ${e.message}")
            false
        }
    }
}
