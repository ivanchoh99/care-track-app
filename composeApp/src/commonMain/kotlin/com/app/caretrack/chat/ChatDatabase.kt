package com.app.caretrack.chat

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<MessageEntity>>

    @Insert
    suspend fun insertMessage(message: MessageEntity)

    @Query("SELECT COUNT(*) FROM messages")
    suspend fun getMessageCount(): Int
}

@Database(entities = [MessageEntity::class], version = 1)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
}

expect fun instantiateDatabaseBuilder(context: Any?): RoomDatabase.Builder<ChatDatabase>

fun getRoomDatabase(
    builder: RoomDatabase.Builder<ChatDatabase>
): ChatDatabase {
    return builder
        .setDriver(BundledSQLiteDriver()) // Importante para KMP
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
