package com.app.caretrack.chat

import com.app.caretrack.chat.network.ApiService
import com.app.caretrack.chat.network.ChatWebSocket
import com.app.caretrack.chat.network.SendMessageRequest
import com.app.caretrack.chat.network.WebSocketMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ChatRepository(
    private val dao: ChatDao,
    private val fileStorageManager: FileStorageManager,
    private val apiService: ApiService = ApiService(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    companion object {
        val VALID_AUDIO_EXT = listOf("mp3", "wav", "m4a", "ogg", "acc", "flac", "opus")
        val VALID_IMAGE_EXT = listOf("jpg", "jpeg", "png", "webp")
        const val MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024
    }

    private val webSocket = ChatWebSocket(apiService, scope)

    val messages: Flow<List<ChatMessage>> = dao.getAllMessages()
        .map { entities -> entities.map { it.toChatMessage() } }

    init {
        scope.launch {
            webSocket.connect()
        }
        scope.launch {
            webSocket.messages.collect { wsMessage ->
                handleWebSocketMessage(wsMessage)
            }
        }
    }

    private suspend fun handleWebSocketMessage(msg: WebSocketMessage) {
        when (msg.type) {
            "response" -> {
                val responseMessage = botMessage(
                    text = msg.content,
                    backendId = msg.messageId
                )
                dao.insertMessage(responseMessage)
            }

            "token" -> {
                // Streaming token - could be used for real-time text display
                AppLogger.d("WebSocket", "Token recibido: ${msg.content}")
            }

            "done" -> {
                AppLogger.d("WebSocket", "Respuesta completa")
            }

            "error" -> {
                AppLogger.e("WebSocket", "Error del servidor: ${msg.content}")
                dao.insertMessage(botMessage("Error al procesar tu mensaje: ${msg.content}"))
            }
        }
    }

    suspend fun ensureWelcomeMessage() {
        if (dao.getMessageCount() == 0) {
            dao.insertMessage(welcomeMessage())
        }
    }

    suspend fun sendMessage(text: String) {
        if (text.isBlank()) return

        val msgId = Uuid.generateV7().toString()
        val userMsg = MessageEntity(
            id = msgId,
            content = text,
            type = MessageType.TEXT.name,
            isMine = true,
            timestamp = Clock.System.now().toEpochMilliseconds(),
            status = MessageStatus.SENDING.name
        )
        dao.insertMessage(userMsg)

        val result = apiService.sendMessage(SendMessageRequest(content = text, type = "text"))
        result.onSuccess {
            dao.updateMessageStatus(msgId, MessageStatus.SENT.name)
            if (it.id.isNotEmpty()) {
                dao.updateMessageStatus(msgId, MessageStatus.SENT.name)
            }
        }.onFailure {
            dao.updateMessageStatus(msgId, MessageStatus.FAILED.name)
        }
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

        if (fileBytes != null && fileBytes.size > MAX_FILE_SIZE_BYTES) {
            dao.insertMessage(botMessage("El archivo excede el límite de 50 MB."))
            return
        }

        val storedPath = if (fileBytes != null) {
            fileStorageManager.saveFile(fileBytes, fileName)
        } else {
            filePath ?: ""
        }

        val msgId = Uuid.generateV7().toString()
        val entity = MessageEntity(
            id = msgId,
            content = fileName,
            type = type.name,
            isMine = true,
            timestamp = Clock.System.now().toEpochMilliseconds(),
            fileName = fileName,
            extension = ext,
            filePath = storedPath,
            status = MessageStatus.SENDING.name
        )
        dao.insertMessage(entity)

        val result = apiService.sendMessage(
            SendMessageRequest(
                content = fileName,
                type = type.name.lowercase(),
                fileName = fileName,
                extension = ext,
                fileUrl = storedPath
            )
        )
        result.onSuccess {
            dao.updateMessageStatus(msgId, MessageStatus.SENT.name)
        }.onFailure {
            dao.updateMessageStatus(msgId, MessageStatus.FAILED.name)
        }
    }

    suspend fun retryMessage(messageId: String) {
        val entity = dao.getMessageById(messageId) ?: return
        dao.updateMessageStatus(messageId, MessageStatus.SENDING.name)

        val msgType = try {
            MessageType.valueOf(entity.type)
        } catch (_: Exception) {
            MessageType.TEXT
        }

        val result = apiService.sendMessage(
            SendMessageRequest(
                content = entity.content,
                type = msgType.name.lowercase(),
                fileName = entity.fileName,
                extension = entity.extension,
                fileUrl = entity.filePath
            )
        )
        result.onSuccess {
            dao.updateMessageStatus(messageId, MessageStatus.SENT.name)
        }.onFailure {
            dao.updateMessageStatus(messageId, MessageStatus.FAILED.name)
        }
    }

    suspend fun deleteMessage(messageId: String) {
        val message = dao.getMessageById(messageId)
        message?.let {
            if (!it.filePath.isNullOrBlank()) {
                fileStorageManager.deleteFileIfExists(it.filePath)
            }
        }
        dao.deleteMessage(messageId)
    }

    private fun welcomeMessage() = MessageEntity(
        id = Uuid.generateV7().toString(),
        content = "¡Bienvenido a CareTrack! 👋\n¿En qué puedo ayudarte hoy?",
        type = MessageType.TEXT.name,
        isMine = false,
        timestamp = Clock.System.now().toEpochMilliseconds(),
        status = MessageStatus.SENT.name
    )

    private fun userMessage(content: String) = MessageEntity(
        id = Uuid.generateV7().toString(),
        content = content,
        type = MessageType.TEXT.name,
        isMine = true,
        timestamp = Clock.System.now().toEpochMilliseconds(),
        status = MessageStatus.PENDING.name
    )

    private fun botMessage(text: String, backendId: String? = null) = MessageEntity(
        id = Uuid.generateV7().toString(),
        content = text,
        type = MessageType.TEXT.name,
        isMine = false,
        timestamp = Clock.System.now().toEpochMilliseconds(),
        status = MessageStatus.SENT.name,
        backendId = backendId
    )

    private fun MessageEntity.toChatMessage() = ChatMessage(
        id = this.id,
        content = this.content,
        type = try { MessageType.valueOf(this.type) } catch (_: Exception) { MessageType.TEXT },
        isMine = this.isMine,
        timestamp = this.timestamp,
        fileName = this.fileName,
        extension = this.extension,
        size = this.size,
        filePath = this.filePath,
        status = try { MessageStatus.valueOf(this.status) } catch (_: Exception) { MessageStatus.PENDING },
        backendId = this.backendId,
        backendUrl = this.backendUrl
    )
}
