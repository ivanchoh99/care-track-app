package com.app.caretrack.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ChatViewModel(private val dao: ChatDao) : ViewModel() {

    companion object {
        val VALID_AUDIO_EXT = listOf("mp3", "wav", "m4a", "ogg", "acc", "flac", "opus")
        val VALID_IMAGE_EXT = listOf("jpg", "jpeg", "png", "webp")
    }

    // Convertimos el Flow de Room en un StateFlow para la UI
    // Esto hace que la lista se actualice sola al insertar en la DB
    val messages: StateFlow<List<ChatMessage>> = dao.getAllMessages()
        .map { entities ->
            if (entities.isEmpty()) {
                insertWelcomeMessage()
                emptyList()
            } else {
                entities.map { it.toChatMessage() }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private fun insertWelcomeMessage() {
        viewModelScope.launch(Dispatchers.IO) {
            val welcome = MessageEntity(
                id = Uuid.generateV7().toString(),
                content = "¡Bienvenido a CareTrack! 👋\n¿En qué puedo ayudarte hoy?",
                type = MessageType.TEXT.name,
                isMine = false,
                timestamp = Clock.System.now().toEpochMilliseconds()
            )
            dao.insertMessage(welcome)
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val userMessage = MessageEntity(
                id = Uuid.generateV7().toString(),
                content = text,
                type = MessageType.TEXT.name,
                isMine = true,
                timestamp = Clock.System.now().toEpochMilliseconds()
            )
            dao.insertMessage(userMessage)

            // Simulación de respuesta
            generateBotResponse("Entendido, he guardado tu mensaje.")
        }
    }

    fun processAndSendFile(
        fileName: String,
        extension: String?,
        type: MessageType,
        filePath: String? = null // Usamos la ruta para no saturar la DB con bytes
    ) {
        val ext = extension?.lowercase() ?: fileName.substringAfterLast(".", "").lowercase()
        val isAllowed = when (type) {
            MessageType.IMAGE -> VALID_IMAGE_EXT.contains(ext)
            MessageType.DOCUMENT -> ext == "pdf"
            MessageType.AUDIO -> VALID_AUDIO_EXT.contains(ext)
            else -> false
        }

        viewModelScope.launch(Dispatchers.IO) {
            if (isAllowed) {
                val entity = MessageEntity(
                    id = Uuid.generateV7().toString(),
                    content = fileName,
                    type = type.name,
                    isMine = true,
                    timestamp = Clock.System.now().toEpochMilliseconds(),
                    fileName = fileName,
                    extension = ext,
                    filePath = filePath // Referencia física al archivo en el celular
                )
                dao.insertMessage(entity)
                generateBotResponse("He recibido tu archivo: $fileName")
            } else {
                generateBotResponse("Lo siento, el formato .$ext no es compatible.")
            }
        }
    }

    private suspend fun generateBotResponse(text: String) {
        val botMessage = MessageEntity(
            id = Uuid.generateV7().toString(),
            content = text,
            type = MessageType.TEXT.name,
            isMine = false,
            timestamp = Clock.System.now().toEpochMilliseconds()
        )
        dao.insertMessage(botMessage)
    }

    // Mapper de Entidad (DB) a Modelo (UI)
    private fun MessageEntity.toChatMessage() = ChatMessage(
        id = Uuid.parse(this.id),
        content = this.content,
        type = MessageType.valueOf(this.type),
        isMine = this.isMine,
        fileName = this.fileName,
        extension = this.extension,
        size = this.size,
        filePath = this.filePath // El reproductor usará esta ruta para "leer" el archivo
    )
}