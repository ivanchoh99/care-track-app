package com.app.caretrack.chat.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// MODELOS DE RED — SERIALIZACIÓN CON KOTLINX.SERIALIZATION
// =============================================================================
// Este archivo define los objetos que se envían y reciben desde el servidor.
//
// `@Serializable` → Hace que la clase pueda convertirse a/desde JSON
//   automáticamente. Es como un contrato entre la app y el servidor.
//
// `@SerialName("nombre_json")` → Mapea un campo Kotlin a un nombre diferente
//   en el JSON. Ej: `fileName` en Kotlin se envía como `"file_name"` en el JSON.
//   Esto permite seguir convenciones JSON (snake_case) sin sacrificar las
//   convenciones Kotlin (camelCase).
//
// Estas clases son específicas de la red. No se usan directamente en la UI;
// el ChatRepository las transforma a ChatMessage o MessageEntity.
// =============================================================================

/**
 * Cuerpo del request HTTP POST para enviar un mensaje al servidor.
 *
 * Se serializa a JSON antes de enviarse:
 * ```json
 * {
 *   "content": "Hola",
 *   "type": "text",
 *   "file_name": null,
 *   "extension": null,
 *   "file_url": null
 * }
 * ```
 *
 * @param content  Texto del mensaje o nombre del archivo.
 * @param type     Tipo en minúsculas: "text", "image", "audio", "document".
 * @param fileName Nombre del archivo (null para mensajes de texto).
 * @param extension Extensión del archivo, ej. "pdf" (null para texto).
 * @param fileUrl  Ruta local o URL del archivo para que el servidor lo descargue.
 */
@Serializable
data class SendMessageRequest(
    val content: String,
    val type: String,
    @SerialName("file_name")
    val fileName: String? = null,
    val extension: String? = null,
    @SerialName("file_url")
    val fileUrl: String? = null
)

/**
 * Respuesta del servidor al enviar un mensaje (POST /api/chat/message).
 *
 * @param id       ID asignado por el servidor al mensaje.
 * @param content  Contenido confirmado del mensaje.
 * @param type     Tipo del mensaje confirmado por el servidor.
 * @param fileUrl  URL pública donde el servidor guardó el archivo (si aplica).
 * @param fileName Nombre del archivo según el servidor.
 * @param error    Mensaje de error del servidor (null si todo salió bien).
 */
@Serializable
data class ApiResponse(
    val id: String,
    val content: String,
    val type: String,
    @SerialName("file_url")
    val fileUrl: String? = null,
    @SerialName("file_name")
    val fileName: String? = null,
    val error: String? = null
)

/**
 * Mensaje recibido por el WebSocket desde el servidor.
 *
 * El servidor usa este mismo formato para varios tipos de eventos:
 * - `"response"` → Respuesta completa del bot (se guarda como mensaje).
 * - `"token"`    → Un fragmento de la respuesta (streaming token a token).
 * - `"done"`     → La respuesta de streaming terminó.
 * - `"error"`    → Ocurrió un error en el servidor.
 *
 * @param type       Tipo del evento WebSocket (response/token/done/error).
 * @param content    Contenido del mensaje o del token.
 * @param messageId  ID del mensaje al que pertenece este evento (opcional).
 * @param tokenIndex Índice del token en una respuesta de streaming (opcional).
 * @param done       `true` cuando el streaming terminó.
 * @param error      Descripción del error (solo cuando type = "error").
 */
@Serializable
data class WebSocketMessage(
    val type: String,
    val content: String,
    @SerialName("message_id")
    val messageId: String? = null,
    @SerialName("token_index")
    val tokenIndex: Int? = null,
    val done: Boolean = false,
    val error: String? = null
)

/**
 * Respuesta del endpoint de health check (GET /health).
 *
 * Se usa para verificar que el servidor está activo antes de operar.
 *
 * @param status  Estado del servidor, ej. "ok" o "healthy".
 * @param version Versión del servidor (opcional, puede no estar presente).
 */
@Serializable
data class HealthResponse(
    val status: String,
    val version: String? = null
)

// TODO: Implementar un tipo sellado (sealed class) para WebSocketMessage.type
//       en lugar de strings libres ("response", "token", etc.). Esto haría
//       el manejo de eventos más seguro en tiempo de compilación y evitaría
//       typos silenciosos.
//
// TODO: Agregar validación de la respuesta del servidor: si `ApiResponse.error`
//       no es null, lanzar una excepción para que `runCatching` la capture.
//       Actualmente el error del servidor se ignora si el HTTP status es 200.
//
// TODO: Cuando se implemente autenticación, agregar headers de autorización
//       (Bearer token) en ApiService, no en los modelos.
