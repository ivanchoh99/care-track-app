package com.app.caretrack.chat

import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSUserDomainMask

actual class FileStorageManager actual constructor(context: Any?) {
    actual fun saveFile(bytes: ByteArray, fileName: String): String {
        val documentsDir = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory, NSUserDomainMask, true
        ).first() as String
        val mediaDir = "$documentsDir/media"
        NSFileManager.defaultManager.createDirectoryAtPath(
            path = mediaDir,
            withIntermediateDirectories = true,
            attributes = null,
            error = null
        )
        val filePath = "$mediaDir/$fileName"
        val data = NSData.create(bytes = bytes, length = bytes.size.toULong())
        data?.writeToFile(filePath, atomically = true)
        return filePath
    }
}
