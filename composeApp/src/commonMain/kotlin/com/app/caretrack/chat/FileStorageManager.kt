package com.app.caretrack.chat

expect class FileStorageManager(context: Any?) {
    fun saveFile(bytes: ByteArray, fileName: String): String
}
