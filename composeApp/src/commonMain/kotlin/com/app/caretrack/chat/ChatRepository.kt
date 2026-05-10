package com.app.caretrack.chat

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ChatRepository(
    private val dao: ChatDao,
    private val fileStorageManager: FileStorageManager
) {
    companion object {
        val VALID_AUDIO_EXT = listOf("mp3", "wav", "m4a", "ogg", "acc", "flac", "opus")
        val VALID_IMAGE_EXT = listOf("jpg", "jpeg", "png", "webp")
    }

    val messages: Flow<List<ChatMessage>> = dao.getAllMessages()
        .map { entities -> entities.map { it.toChatMessage() } }

    suspend fun ensureWelcomeMessage() {
        if (dao.getMessageCount() == 0) {
            dao.insertMessage(welcomeMessage())
        }
    }

    suspend fun sendMessage(text: String) {
        if (text.isBlank()) return
        dao.insertMessage(userMessage(content = text))
        dao.insertMessage(botMessage("Entendido, he guardado tu mensaje."))
    }

    suspend fun processAndSendFile(
        fileName: String,
        extension: String?,
        type: MessageType,
        filePath: String? = null,
        fileBytes: ByteArray? = null
    ) {
        val ext = extension?.lowercase() ?: fileName.substringAfterLast(".", "").lowercase()
        val isAllowed = when (type) {
            MessageType.IMAGE -> VALID_IMAGE_EXT.contains(ext)
            MessageType.DOCUMENT -> ext == "pdf"
            MessageType.AUDIO -> VALID_AUDIO_EXT.contains(ext)
            else -> false
        }

        if (!isAllowed) {
            dao.insertMessage(botMessage("Lo siento, el formato .$ext no es compatible."))
            return
        }

        val storedPath = if (fileBytes != null) {
            fileStorageManager.saveFile(fileBytes, fileName)
        } else {
            filePath
        }

        val entity = MessageEntity(
            id = Uuid.generateV7().toString(),
            content = fileName,
            type = type.name,
            isMine = true,
            timestamp = Clock.System.now().toEpochMilliseconds(),
            fileName = fileName,
            extension = ext,
            filePath = storedPath
        )
        dao.insertMessage(entity)
        dao.insertMessage(botMessage("He recibido tu archivo: $fileName"))
    }

    private fun welcomeMessage() = MessageEntity(
        id = Uuid.generateV7().toString(),
        content = "¡Bienvenido a CareTrack! 👋\n¿En qué puedo ayudarte hoy?",
        type = MessageType.TEXT.name,
        isMine = false,
        timestamp = Clock.System.now().toEpochMilliseconds()
    )

    private fun userMessage(content: String) = MessageEntity(
        id = Uuid.generateV7().toString(),
        content = content,
        type = MessageType.TEXT.name,
        isMine = true,
        timestamp = Clock.System.now().toEpochMilliseconds()
    )

    private fun botMessage(text: String) = MessageEntity(
        id = Uuid.generateV7().toString(),
        content = text,
        type = MessageType.TEXT.name,
        isMine = false,
        timestamp = Clock.System.now().toEpochMilliseconds()
    )

    private fun MessageEntity.toChatMessage() = ChatMessage(
        id = this.id,
        content = this.content,
        type = MessageType.valueOf(this.type),
        isMine = this.isMine,
        fileName = this.fileName,
        extension = this.extension,
        size = this.size,
        filePath = this.filePath
    )
}
