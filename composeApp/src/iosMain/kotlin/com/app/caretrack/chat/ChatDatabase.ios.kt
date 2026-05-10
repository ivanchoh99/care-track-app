package com.app.caretrack.chat

// For iOS, we don't have a direct Room equivalent, so this is a stub
// In a real app, you'd use SQLDelight or a native solution
actual fun instantiateDatabaseBuilder(context: Any?): androidx.room.RoomDatabase.Builder<ChatDatabase> {
    throw NotImplementedError("Room database is not available on iOS in the same way")
}
