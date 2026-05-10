package com.app.caretrack.chat

import androidx.room.Room
import androidx.room.RoomDatabase
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSUserDomainMask

actual fun instantiateDatabaseBuilder(context: Any?): RoomDatabase.Builder<ChatDatabase> {
    val documentsDirectory = NSSearchPathForDirectoriesInDomains(
        NSDocumentDirectory, NSUserDomainMask, true
    ).first() as String
    val dbPath = "$documentsDirectory/caretrack_chat.db"
    return Room.databaseBuilder<ChatDatabase>(name = dbPath)
}
