package com.app.caretrack.chat.network

// =============================================================================
// RESULTADO DE SUBIDA DE ARCHIVOS
// =============================================================================
// Esta clase modela el resultado de una operación de subida de archivo.
// Sigue el patrón "Result Object": en lugar de lanzar excepciones o devolver
// null, devuelves un objeto que describe si la operación fue exitosa o no,
// incluyendo el error si falló.
// =============================================================================

/**
 * Encapsula el resultado de subir un archivo al servidor.
 *
 * Uso típico:
 * ```kotlin
 * val result = uploadFile(bytes, "foto.jpg")
 * if (result.success) {
 *     mostrarUrl(result.url)
 * } else {
 *     mostrarError(result.error ?: "Error desconocido")
 * }
 * ```
 *
 * @param url      URL pública del archivo en el servidor (válida solo si success=true).
 * @param fileName Nombre del archivo tal como lo guardó el servidor.
 * @param success  `true` si la subida fue exitosa, `false` si falló.
 * @param error    Descripción del error (null si success=true).
 */
data class FileUploadResult(
    val url: String,
    val fileName: String,
    val success: Boolean,
    val error: String? = null
)

// TODO: Esta clase está definida pero no se usa en ningún lugar del código actual.
//       Integrarla en ApiService para reemplazar el uso directo de ApiResponse
//       cuando el mensaje contiene un archivo, o eliminarla si no se va a usar.
//
// TODO: Considerar reemplazar esta clase con el tipo estándar de Kotlin
//       `Result<String>` (donde String es la URL), que ya maneja éxito/fracaso
//       sin necesidad de un campo `success` manual.
