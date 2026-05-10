package com.app.caretrack.chat

import com.app.caretrack.chat.network.ApiService
import com.app.caretrack.chat.network.ChatWebSocket
import com.app.caretrack.chat.network.SendMessageRequest
import com.app.caretrack.chat.network.WebSocketMessage
import com.app.caretrack.media.file.FileStorageManager
import com.app.caretrack.media.file.deleteFileIfExists
import com.app.caretrack.common.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

// =============================================================================
// CAPA DE NEGOCIO — REPOSITORIO DE CHAT
// =============================================================================
// El Repository es el intermediario entre los datos (BD + red) y la UI (ViewModel).
// Sigue el patrón Repository de la arquitectura limpia (Clean Architecture).
//
// Responsabilidades del repositorio:
//   1. Obtener mensajes de Room y transformarlos al modelo de UI (ChatMessage)
//   2. Enviar mensajes nuevos: guardar localmente → llamar API → actualizar estado
//   3. Procesar archivos multimedia: validar → guardar → enviar al servidor
//   4. Recibir respuestas del bot vía WebSocket y guardarlas en Room
//   5. Gestionar reintentos de mensajes fallidos
//   6. Eliminar mensajes y sus archivos del disco
//
// Flujo de un mensaje enviado:
//   Usuario escribe → sendMessage() → guardar como SENDING en Room
//   → llamar API REST → actualizar a SENT o FAILED en Room
//   → servidor responde por WebSocket → guardar respuesta del bot en Room
//   → Flow emite la lista actualizada → UI se actualiza automáticamente
//
// Patrón "offline-first":
//   Primero se guarda en local (Room), luego se sincroniza con el servidor.
//   Así el mensaje aparece inmediatamente en la UI aunque la red sea lenta.
// =============================================================================

@OptIn(ExperimentalUuidApi::class)
class ChatRepository(
    private val dao: ChatDao,
    private val fileStorageManager: FileStorageManager,
    // Valores por defecto: permiten crear un ChatRepository básico sin pasar estos parámetros,
    // útil para tests. En producción siempre se pasan desde MainActivity.
    private val apiService: ApiService = ApiService(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    companion object {
        // Extensiones de archivo permitidas por tipo.
        // "acc" en VALID_AUDIO_EXT debería ser "aac" — ver TODO al final.
        val VALID_AUDIO_EXT = listOf("mp3", "wav", "m4a", "ogg", "acc", "flac", "opus")
        val VALID_IMAGE_EXT = listOf("jpg", "jpeg", "png", "webp")

        // 50 MB en bytes: 50 × 1024 (KB) × 1024 (MB)
        const val MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024
    }

    // El WebSocket se crea al instanciar el repositorio (en el init{} de abajo)
    private val webSocket = ChatWebSocket(apiService, scope)

    /**
     * Flow de mensajes del chat, ordenados por timestamp.
     *
     * `.map { entities → }` transforma cada lista de MessageEntity
     * (modelo de BD) a una lista de ChatMessage (modelo de UI).
     *
     * Como `getAllMessages()` devuelve un `Flow`, este `.map` también
     * devuelve un Flow: cada vez que la BD cambia, emite la lista transformada.
     * La UI solo tiene que observar este Flow; nunca consulta la BD directamente.
     */
    val messages: Flow<List<ChatMessage>> = dao.getAllMessages()
        .map { entities -> entities.map { it.toChatMessage() } }

    init {
        // Al crear el repositorio, conectar el WebSocket inmediatamente
        scope.launch {
            webSocket.connect()
        }
        // Y empezar a escuchar mensajes del servidor en otra coroutine
        scope.launch {
            webSocket.messages.collect { wsMessage ->
                handleWebSocketMessage(wsMessage)
            }
        }
    }

    /**
     * Procesa cada mensaje recibido desde el WebSocket del servidor.
     *
     * El servidor puede enviar cuatro tipos de eventos:
     * - `"response"` → respuesta completa del bot (la más común): se guarda en Room.
     * - `"token"`    → un fragmento de respuesta (streaming): solo se loguea por ahora.
     * - `"done"`     → señal de que el streaming terminó: solo se loguea.
     * - `"error"`    → el servidor tuvo un error: se guarda un mensaje de error en Room.
     *
     * `when` en Kotlin es el equivalente mejorado del `switch` de Java/C.
     * Puede usarse tanto como expresión (devuelve un valor) como sentencia.
     */
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
                // TODO: Implementar streaming real de tokens: acumular tokens y
                //       actualizar el mensaje del bot en tiempo real en la UI.
                //       Actualmente los tokens se descartan y solo se muestra la
                //       respuesta completa cuando llega el tipo "response".
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

    /**
     * Inserta el mensaje de bienvenida si la base de datos está vacía.
     *
     * Se llama desde el `init{}` de ChatViewModel al iniciar la app.
     * `getMessageCount() == 0` detecta si es la primera vez que se abre la app
     * (o si se borró toda la BD).
     */
    suspend fun ensureWelcomeMessage() {
        if (dao.getMessageCount() == 0) {
            dao.insertMessage(welcomeMessage())
        }
    }

    /**
     * Envía un mensaje de texto al servidor y lo guarda localmente.
     *
     * Flujo:
     * 1. Validar que el texto no esté vacío.
     * 2. Crear un MessageEntity con estado SENDING y guardarlo en Room.
     *    → La UI muestra el mensaje inmediatamente con un indicador de "enviando".
     * 3. Llamar a la API REST para enviar el mensaje.
     * 4. Actualizar el estado en Room a SENT o FAILED según el resultado.
     *
     * `result.onSuccess { } .onFailure { }` es la API funcional de Kotlin
     * para manejar un `Result<T>` (equivalente al try-catch pero más compacto).
     *
     * @param text El texto del mensaje (no puede ser vacío o solo espacios).
     */
    suspend fun sendMessage(text: String) {
        if (text.isBlank()) return  // Early return: no enviar mensajes vacíos

        // Uuid.generateV7() genera un UUID ordenable temporalmente (basado en timestamp).
        // Es preferible a V4 (aleatorio) porque permite ordenar por ID además de por timestamp.
        val msgId = Uuid.generateV7().toString()
        val userMsg = MessageEntity(
            id = msgId,
            content = text,
            type = MessageType.TEXT.name,
            isMine = true,
            timestamp = Clock.System.now().toEpochMilliseconds(),
            status = MessageStatus.SENDING.name
        )
        dao.insertMessage(userMsg)  // Guardar localmente primero (offline-first)

        // Intentar enviar al servidor
        val result = apiService.sendMessage(SendMessageRequest(content = text, type = "text"))
        result.onSuccess {
            // TODO: El estado se actualiza dos veces seguidas a SENT cuando el id no está vacío.
            //       La segunda llamada es redundante — eliminar el bloque `if (it.id.isNotEmpty())`.
            dao.updateMessageStatus(msgId, MessageStatus.SENT.name)
            if (it.id.isNotEmpty()) {
                dao.updateMessageStatus(msgId, MessageStatus.SENT.name)
            }
        }.onFailure {
            dao.updateMessageStatus(msgId, MessageStatus.FAILED.name)
        }
    }

    /**
     * Procesa y envía un archivo multimedia (imagen, audio, PDF).
     *
     * Flujo:
     * 1. Validar que la extensión sea compatible con el tipo declarado.
     * 2. Validar que el archivo no supere 50 MB.
     * 3. Guardar el archivo localmente (si viene como ByteArray).
     * 4. Crear MessageEntity con la ruta local y estado SENDING.
     * 5. Llamar a la API con metadatos del archivo.
     * 6. Actualizar estado a SENT o FAILED.
     *
     * @param fileName  Nombre original del archivo (ej. "foto_vacuna.jpg").
     * @param extension Extensión del archivo. Si es null, se extrae del fileName.
     * @param type      Tipo declarado por la UI (IMAGE, AUDIO, DOCUMENT).
     * @param filePath  Ruta local si el archivo ya existe en disco (ej. notas de voz grabadas).
     * @param fileBytes Contenido binario si el archivo se seleccionó del picker (aún no guardado).
     */
    suspend fun processAndSendFile(
        fileName: String,
        extension: String?,
        type: MessageType,
        filePath: String? = null,
        fileBytes: ByteArray? = null
    ) {
        // Normalizar la extensión: preferir la pasada explícitamente, si no extraer del nombre
        val ext = extension?.lowercase() ?: fileName.substringAfterLast(".", "").lowercase()

        // Validar que la extensión sea compatible con el tipo declarado
        val isAllowed = when (type) {
            MessageType.IMAGE    -> VALID_IMAGE_EXT.contains(ext)
            MessageType.DOCUMENT -> ext == "pdf"
            MessageType.AUDIO    -> VALID_AUDIO_EXT.contains(ext)
            else -> false  // TEXT no se procesa como archivo
        }

        if (!isAllowed) {
            // Informar al usuario con un mensaje del "bot" (isMine = false)
            dao.insertMessage(botMessage("Lo siento, el formato .$ext no es compatible."))
            return
        }

        // Validar tamaño (solo si tenemos los bytes; si es una ruta local, no verificamos)
        if (fileBytes != null && fileBytes.size > MAX_FILE_SIZE_BYTES) {
            dao.insertMessage(botMessage("El archivo excede el límite de 50 MB."))
            return
        }

        // Guardar el archivo localmente:
        // - Si tenemos bytes (picker de archivos) → guardar en FileStorageManager
        // - Si tenemos ruta (audio grabado) → ya está guardado, usamos la ruta directamente
        val storedPath = if (fileBytes != null) {
            fileStorageManager.saveFile(fileBytes, fileName)
        } else {
            filePath ?: ""
        }

        val msgId = Uuid.generateV7().toString()
        val entity = MessageEntity(
            id = msgId,
            content = fileName,     // Para multimedia, el "content" es el nombre del archivo
            type = type.name,
            isMine = true,
            timestamp = Clock.System.now().toEpochMilliseconds(),
            fileName = fileName,
            extension = ext,
            filePath = storedPath,  // Ruta local para mostrar en la UI sin internet
            status = MessageStatus.SENDING.name
        )
        dao.insertMessage(entity)

        // Enviar al servidor con metadatos del archivo
        val result = apiService.sendMessage(
            SendMessageRequest(
                content = fileName,
                type = type.name.lowercase(),  // "image", "audio", "document"
                fileName = fileName,
                extension = ext,
                fileUrl = storedPath           // El servidor usa esta ruta para acceder al archivo
            )
        )
        result.onSuccess {
            dao.updateMessageStatus(msgId, MessageStatus.SENT.name)
        }.onFailure {
            dao.updateMessageStatus(msgId, MessageStatus.FAILED.name)
        }
    }

    /**
     * Reintenta el envío de un mensaje que falló previamente.
     *
     * Pasos:
     * 1. Buscar el mensaje en la BD por ID.
     * 2. Cambiar su estado a SENDING.
     * 3. Reenviar al servidor con los mismos datos originales.
     * 4. Actualizar estado a SENT o FAILED.
     *
     * El try-catch con `_: Exception` (el guion bajo ignora la variable de excepción)
     * es una forma idiomática de Kotlin para ignorar un valor que no necesitas.
     */
    suspend fun retryMessage(messageId: String) {
        val entity = dao.getMessageById(messageId) ?: return  // Si no existe, no hacer nada

        dao.updateMessageStatus(messageId, MessageStatus.SENDING.name)

        // Intentar parsear el tipo; si la BD tiene un valor corrupto, usar TEXT por defecto
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

    /**
     * Elimina un mensaje de la BD y su archivo del disco (si tiene uno).
     *
     * El orden importa: primero obtenemos el filePath de la BD, luego borramos
     * el archivo del disco, y finalmente borramos la fila de la BD.
     * Si lo hiciéramos al revés, perderíamos la ruta del archivo.
     */
    suspend fun deleteMessage(messageId: String) {
        val message = dao.getMessageById(messageId)
        message?.let {
            if (!it.filePath.isNullOrBlank()) {
                // `deleteFileIfExists` es una extension function de FileStorageManager
                // que verifica que el path no sea null antes de intentar borrar
                fileStorageManager.deleteFileIfExists(it.filePath)
            }
        }
        dao.deleteMessage(messageId)
    }

    // =========================================================================
    // FUNCIONES AUXILIARES PRIVADAS (fábricas de mensajes)
    // =========================================================================
    // Estas funciones siguen el patrón "factory": crean objetos preconfigurados
    // para casos de uso específicos, evitando repetir la misma configuración.

    /** Crea el mensaje de bienvenida que aparece cuando el chat está vacío. */
    private fun welcomeMessage() = MessageEntity(
        id = Uuid.generateV7().toString(),
        content = "¡Bienvenido a CareTrack! 👋\n¿En qué puedo ayudarte hoy?",
        type = MessageType.TEXT.name,
        isMine = false,  // Aparece como mensaje del bot (burbuja izquierda)
        timestamp = Clock.System.now().toEpochMilliseconds(),
        status = MessageStatus.SENT.name
    )

    /**
     * Crea un mensaje de usuario de texto. Actualmente no se usa en ningún lugar.
     * `userMessage()` fue reemplazada por código inline en `sendMessage()`.
     */
    private fun userMessage(content: String) = MessageEntity(
        id = Uuid.generateV7().toString(),
        content = content,
        type = MessageType.TEXT.name,
        isMine = true,
        timestamp = Clock.System.now().toEpochMilliseconds(),
        status = MessageStatus.PENDING.name
    )

    /**
     * Crea un mensaje del bot (respuesta del servidor o mensaje de error del sistema).
     *
     * @param text      Contenido del mensaje.
     * @param backendId ID asignado por el servidor (opcional).
     */
    private fun botMessage(text: String, backendId: String? = null) = MessageEntity(
        id = Uuid.generateV7().toString(),
        content = text,
        type = MessageType.TEXT.name,
        isMine = false,
        timestamp = Clock.System.now().toEpochMilliseconds(),
        status = MessageStatus.SENT.name,
        backendId = backendId
    )

    /**
     * Convierte un [MessageEntity] (modelo de BD) a [ChatMessage] (modelo de UI).
     *
     * Extension function privada: `fun MessageEntity.toChatMessage()` añade este
     * método solo a las instancias de MessageEntity dentro de ChatRepository.
     *
     * Los `try-catch` en los enums son necesarios porque en la BD están guardados
     * como String. Si alguien modifica la BD directamente o hay un bug, el valor
     * puede no corresponder a ningún enum válido → fallback a valores seguros.
     */
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

// TODO: CRÍTICO — La función `sendMessage()` llama a `updateMessageStatus` dos
//       veces seguidas con el mismo valor cuando la respuesta tiene id no vacío.
//       Eliminar la llamada duplicada dentro del bloque `if (it.id.isNotEmpty())`.
//
// TODO: VALID_AUDIO_EXT contiene "acc" que debería ser "aac" (AAC = Advanced Audio Coding).
//       Con este typo, los archivos .aac no son aceptados por la app.
//
// TODO: La función privada `userMessage()` existe pero nunca se usa (el código
//       de sendMessage() crea el MessageEntity inline). Eliminarla para limpiar el código.
//
// TODO: El CoroutineScope creado con `CoroutineScope(Dispatchers.IO)` no está ligado
//       a ningún ciclo de vida. Si el ChatRepository se recrea (ej. rotación de pantalla),
//       el scope anterior queda activo indefinidamente. El scope debería venir del
//       ViewModel (viewModelScope) que sí se cancela automáticamente.
//
// TODO: Cuando se implementen múltiples conversaciones, agregar un parámetro
//       `conversationId` a sendMessage(), processAndSendFile(), y las queries del DAO.
//
// TODO: Agregar manejo del campo `backendUrl` en processAndSendFile(): cuando el
//       servidor devuelve la URL pública del archivo, guardarla en Room con
//       `dao.updateMessageBackendUrl(msgId, it.fileUrl)`. Actualmente se ignora.
