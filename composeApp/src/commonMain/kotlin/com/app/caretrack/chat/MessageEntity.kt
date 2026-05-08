package com.app.caretrack.chat

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val content: String,
    val type: String,
    val isMine: Boolean,
    val timestamp: Long,
    val fileName: String? = null,
    val extension: String? = null,
    val size: String? = null,
    val filePath: String? = null // La ruta donde se encuentra guardado el archivo/audio
)