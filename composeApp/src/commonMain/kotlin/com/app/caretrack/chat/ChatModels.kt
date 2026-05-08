package com.app.caretrack.chat

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class MessageType { TEXT, IMAGE, AUDIO, DOCUMENT }

data class ChatMessage @OptIn(ExperimentalUuidApi::class) constructor(
    val id: String = Uuid.generateV7().toString(), // Guardado como String directamente
    val content: String,
    val type: MessageType,
    val isMine: Boolean,
    val fileName: String? = null,
    val extension: String? = null,
    val size: String? = null,
    val filePath: String? = null // Única fuente de verdad para los archivos físicos
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ChatMessage

        if (id != other.id) return false
        if (content != other.content) return false
        if (type != other.type) return false
        if (isMine != other.isMine) return false
        if (fileName != other.fileName) return false
        if (extension != other.extension) return false
        if (size != other.size) return false
        if (filePath != other.filePath) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + content.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + isMine.hashCode()
        result = 31 * result + (fileName?.hashCode() ?: 0)
        result = 31 * result + (extension?.hashCode() ?: 0)
        result = 31 * result + (size?.hashCode() ?: 0)
        result = 31 * result + (filePath?.hashCode() ?: 0)
        return result
    }
}