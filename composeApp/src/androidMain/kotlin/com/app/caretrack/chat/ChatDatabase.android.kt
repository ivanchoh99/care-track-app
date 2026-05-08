package com.app.caretrack.chat

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

// Aquí implementas el 'actual'
actual fun instantiateDatabaseBuilder(context: Any?): RoomDatabase.Builder<ChatDatabase> {
    val appContext = (context as Context).applicationContext

    // Esto le dice a Room que cree el archivo en la carpeta interna de la app
    val dbFile = appContext.getDatabasePath("caretrack_chat.db")

    return Room.databaseBuilder<ChatDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    )
}