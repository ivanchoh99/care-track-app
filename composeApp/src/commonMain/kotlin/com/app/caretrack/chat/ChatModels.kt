package com.app.caretrack.chat

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

// =============================================================================
// MODELOS DE DOMINIO DEL CHAT
// =============================================================================
// Este archivo define las estructuras de datos que usa la UI para representar
// mensajes. Son distintas de las entidades de base de datos (MessageEntity).
//
// ¿Por qué tener dos modelos separados (ChatMessage y MessageEntity)?
// → MessageEntity es el modelo de la base de datos (Room). Room necesita
//   tipos simples como String para los enums (ej. "TEXT", "AUDIO").
// → ChatMessage es el modelo de la UI. Usa enums reales (MessageType.TEXT)
//   lo que hace el código más seguro y legible.
// → Esta separación se llama "separación de capas" y es una buena práctica.
// =============================================================================

/**
 * Enum que representa el tipo de contenido de un mensaje.
 *
 * En Kotlin, un `enum class` es una lista fija de valores posibles.
 * Úsalo cuando algo solo puede ser una de N opciones concretas.
 *
 * Tipos soportados:
 * - TEXT     → Mensaje de texto plano
 * - IMAGE    → Imagen (jpg, png, webp)
 * - AUDIO    → Nota de voz o archivo de audio
 * - DOCUMENT → Documento PDF
 */
enum class MessageType { TEXT, IMAGE, AUDIO, DOCUMENT }

/**
 * Enum que representa el estado de envío de un mensaje del usuario.
 *
 * El ciclo de vida de un mensaje es:
 *   PENDING → SENDING → SENT
 *                    ↘ FAILED  (si el servidor devuelve error)
 *
 * PENDING:  Creado localmente, aún no se intentó enviar.
 * SENDING:  Se está enviando al servidor en este momento.
 * SENT:     El servidor confirmó la recepción exitosamente.
 * FAILED:   El envío falló; el usuario puede reintentar.
 *
 * Los mensajes del bot (isMine = false) siempre llegan como SENT,
 * ya que provienen del servidor directamente.
 */
enum class MessageStatus { PENDING, SENDING, SENT, FAILED }

/**
 * Modelo de mensaje usado en la capa de UI (Compose).
 *
 * Es una `data class`, que en Kotlin genera automáticamente:
 * - equals() / hashCode() → compara por valor, no por referencia
 * - toString()            → representación legible
 * - copy()                → crea una copia modificando solo algunos campos
 *
 * @param id        Identificador único (UUID v7, que es cronológicamente ordenable).
 * @param content   Texto del mensaje o nombre del archivo adjunto.
 * @param type      Tipo de contenido (TEXT, IMAGE, AUDIO, DOCUMENT).
 * @param isMine    `true` si el mensaje lo envió el usuario, `false` si es del bot.
 * @param timestamp Momento del mensaje en milisegundos desde epoch (Unix timestamp).
 * @param fileName  Nombre original del archivo (solo para mensajes multimedia).
 * @param extension Extensión del archivo, ej. "pdf", "mp3" (solo multimedia).
 * @param size      Tamaño legible del archivo, ej. "2.3 MB" (opcional).
 * @param filePath  Ruta local donde está guardado el archivo en el dispositivo.
 * @param status    Estado actual del mensaje (ver [MessageStatus]).
 * @param backendId ID asignado por el servidor al mensaje (puede ser distinto al local).
 * @param backendUrl URL del servidor donde está alojado el archivo (cuando aplica).
 */
data class ChatMessage @OptIn(ExperimentalUuidApi::class) constructor(
    // @OptIn(ExperimentalUuidApi::class) le dice al compilador que sabemos que
    // estamos usando una API marcada como experimental y que aceptamos los riesgos.
    val id: String = Uuid.generateV7().toString(),
    val content: String,
    val type: MessageType,
    val isMine: Boolean,
    val timestamp: Long = 0L,
    val fileName: String? = null,   // El '?' significa que puede ser null (nullable)
    val extension: String? = null,
    val size: String? = null,
    val filePath: String? = null,
    val status: MessageStatus = MessageStatus.PENDING,
    val backendId: String? = null,
    val backendUrl: String? = null
)

// TODO: Considerar separar los campos multimedia (fileName, extension, size, filePath,
//       backendUrl) en una data class anidada "MediaAttachment" para que ChatMessage
//       sea más limpio. Los mensajes de tipo TEXT nunca usan esos campos.
//
// TODO: Agregar un campo `conversationId: String` cuando se implemente el soporte
//       para múltiples conversaciones (actualmente solo hay una).
//
// TODO: Evaluar usar kotlinx.datetime.Instant en lugar de Long para el timestamp,
//       ya que Instant es multiplataforma, tipado y más expresivo.
