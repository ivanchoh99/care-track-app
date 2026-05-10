package com.app.caretrack.chat

import android.content.Context
import androidx.room.Room

actual fun instantiateDatabaseBuilder(context: Any?): androidx.room.RoomDatabase.Builder<ChatDatabase> {
    val androidContext = context as? Context
    return Room.databaseBuilder(
        androidContext!!,
        ChatDatabase::class.java,
        "caretrack_chat_database"
    )
}
