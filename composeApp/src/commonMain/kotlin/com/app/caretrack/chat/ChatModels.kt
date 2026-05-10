package com.app.caretrack.chat

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class MessageType { TEXT, IMAGE, AUDIO, DOCUMENT }
enum class MessageStatus { PENDING, SENDING, SENT, FAILED }

data class ChatMessage @OptIn(ExperimentalUuidApi::class) constructor(
    val id: String = Uuid.generateV7().toString(),
    val content: String,
    val type: MessageType,
    val isMine: Boolean,
    val timestamp: Long = 0L,
    val fileName: String? = null,
    val extension: String? = null,
    val size: String? = null,
    val filePath: String? = null,
    val status: MessageStatus = MessageStatus.PENDING,
    val backendId: String? = null,
    val backendUrl: String? = null
)
