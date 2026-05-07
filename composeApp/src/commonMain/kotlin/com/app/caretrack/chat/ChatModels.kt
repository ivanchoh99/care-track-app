package com.app.caretrack.chat

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class MessageType { TEXT, IMAGE, AUDIO, DOCUMENT }

data class ChatMessage @OptIn(ExperimentalUuidApi::class) constructor(
    val id: String = Uuid.generateV7().toString(),
    val content: String, // Texto o ruta del archivo
    val type: MessageType,
    val isMine: Boolean,
    val fileName: String? = null // Para PDFs o audios
)
