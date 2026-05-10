package com.app.caretrack.chat

import androidx.room.Entity
import androidx.room.PrimaryKey

// =============================================================================
// ENTIDAD DE BASE DE DATOS — ROOM
// =============================================================================
// Room es la librería de persistencia local de Android/KMP. Convierte clases
// Kotlin en tablas SQLite automáticamente usando anotaciones (las @Anotaciones).
//
// Flujo de datos:
//   Red/Usuario → MessageEntity (se guarda en SQLite)
//               → ChatMessage  (se convierte para la UI en ChatRepository)
//
// La separación entre MessageEntity y ChatMessage es importante:
// Room solo puede guardar tipos primitivos (String, Long, Boolean, Int).
// Por eso los enums se guardan como String: "TEXT", "AUDIO", etc.
// En la UI usamos los enums reales (MessageType.TEXT) que son más seguros.
// =============================================================================

/**
 * Representa una fila en la tabla `messages` de la base de datos SQLite.
 *
 * @Entity   → Le dice a Room que esta clase es una tabla de la base de datos.
 *             El nombre de la tabla se define con `tableName`.
 *
 * @PrimaryKey → Marca el campo que identifica unívocamente cada fila (como el
 *               "id" de una tabla SQL). No puede haber dos mensajes con el mismo id.
 *
 * @param id        UUID v7 del mensaje (string). Es la llave primaria de la tabla.
 * @param content   Texto del mensaje o nombre del archivo si es multimedia.
 * @param type      Tipo guardado como String: "TEXT", "IMAGE", "AUDIO", "DOCUMENT".
 * @param isMine    `true` = mensaje del usuario, `false` = mensaje del bot.
 * @param timestamp Marca de tiempo en milisegundos (Unix timestamp).
 * @param fileName  Nombre original del archivo (null si es texto).
 * @param extension Extensión del archivo, ej. "pdf", "wav" (null si es texto).
 * @param size      Tamaño legible, ej. "1.2 MB" (actualmente no se calcula).
 * @param filePath  Ruta absoluta local del archivo guardado en el dispositivo.
 * @param status    Estado como String: "PENDING", "SENDING", "SENT", "FAILED".
 * @param backendId ID que el servidor asigna al mensaje (puede diferir del local).
 * @param backendUrl URL del servidor donde está el archivo (para reproducción remota).
 */
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
    val filePath: String? = null,
    val status: String = MessageStatus.PENDING.name,  // .name convierte el enum a String
    val backendId: String? = null,
    val backendUrl: String? = null
)

// TODO: Cuando se implemente autenticación, agregar `userId: String` para
//       poder filtrar mensajes por usuario y soportar múltiples cuentas.
//
// TODO: Agregar `conversationId: String` para soportar múltiples conversaciones.
//       Actualmente toda la app tiene una sola conversación global.
//
// TODO: El campo `size` nunca se calcula ni se guarda en esta versión.
//       Implementar el cálculo del tamaño al guardar el archivo en FileStorageManager
//       y almacenarlo aquí en formato legible ("1.2 MB").
//
// TODO: Considerar una migración de Room (Migration class) en lugar de usar
//       `fallbackToDestructiveMigration` al cambiar el esquema. Esto evita
//       perder datos del usuario al actualizar la app.
