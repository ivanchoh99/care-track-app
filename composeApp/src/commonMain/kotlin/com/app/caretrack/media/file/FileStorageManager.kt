package com.app.caretrack.media.file

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

expect class FileStorageManager(context: Any?) {
    fun saveFile(bytes: ByteArray, fileName: String): String
    fun deleteFile(path: String): Boolean
}

suspend fun FileStorageManager.deleteFileIfExists(path: String?): Boolean {
    if (path.isNullOrBlank()) return false
    return withContext(Dispatchers.IO) {
        deleteFile(path)
    }
}
