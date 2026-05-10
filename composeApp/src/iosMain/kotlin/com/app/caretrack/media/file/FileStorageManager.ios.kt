package com.app.caretrack.media.file

import com.app.caretrack.common.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

// =============================================================================
// IMPLEMENTACIÓN iOS DE FileStorageManager (actual) — STUB PENDIENTE
// =============================================================================
// En iOS, el sistema de archivos del sandbox de la app tiene varios directorios:
//
//   Documents/   → para archivos creados por el usuario (se sincronizan con iCloud,
//                  el usuario puede verlos en la app "Archivos" de iOS)
//   Library/     → para datos de la app (configuración, caché, BD)
//   tmp/         → para archivos temporales (el sistema los puede borrar)
//
// Para archivos multimedia del chat, el directorio correcto es:
//   Library/Application Support/media/   (persiste entre actualizaciones, no visible al usuario)
//
// La API para acceder a estos paths en Kotlin/Native iOS es:
//   NSSearchPathForDirectoriesInDomains(NSLibraryDirectory, NSUserDomainMask, true)
// =============================================================================

/**
 * ⚠️ STUB PENDIENTE — Implementación iOS de [FileStorageManager].
 *
 * Actualmente no guarda ni borra ningún archivo. Devuelve valores vacíos/false.
 * Esto significa que los archivos multimedia NO funcionan en iOS.
 *
 * @param context No se usa en iOS (el sandbox no requiere un Context como Android).
 */
actual class FileStorageManager(context: Any?) {

    /**
     * ⚠️ SIN IMPLEMENTAR — Debería guardar el archivo en el sandbox de iOS.
     *
     * La implementación correcta en Kotlin/Native:
     * ```kotlin
     * import platform.Foundation.*
     *
     * actual fun saveFile(bytes: ByteArray, fileName: String): String {
     *     val libraryDir = NSSearchPathForDirectoriesInDomains(
     *         NSLibraryDirectory, NSUserDomainMask, true
     *     ).first() as String
     *     val mediaDir = "$libraryDir/media"
     *     NSFileManager.defaultManager.createDirectoryAtPath(mediaDir, ...)
     *     val filePath = "$mediaDir/$fileName"
     *     NSData.dataWithBytes(bytes.toRefArray(), bytes.size.toULong())
     *         .writeToFile(filePath, atomically = true)
     *     return filePath
     * }
     * ```
     */
    actual fun saveFile(bytes: ByteArray, fileName: String): String {
        // TODO: Implementar guardado de archivos en iOS con NSFileManager
        AppLogger.w("FileStorage", "saveFile called on iOS - not implemented")
        return ""  // ← Devuelve vacío: los archivos no se guardan en iOS
    }

    /**
     * ⚠️ SIN IMPLEMENTAR — Debería eliminar el archivo del sandbox de iOS.
     */
    actual fun deleteFile(path: String): Boolean {
        // TODO: Implementar con NSFileManager.defaultManager.removeItemAtPath(path)
        AppLogger.w("FileStorage", "deleteFile called on iOS - not implemented")
        return false
    }
}

// Nota: esta extension function está definida también en el commonMain.
// La versión iOS redefine la función porque el archivo compila de forma
// independiente en iosMain. Idealmente solo debería estar en commonMain.
suspend fun FileStorageManager.deleteFileIfExists(path: String?): Boolean {
    if (path.isNullOrBlank()) return false
    return withContext(Dispatchers.IO) {
        deleteFile(path)
    }
}

// TODO: CRÍTICO para soporte iOS — Implementar saveFile() y deleteFile()
//       con NSFileManager (Foundation). Sin esto, ningún archivo multimedia
//       (imágenes, audios, PDFs) funcionará en iPhone/iPad.
//
// TODO: Eliminar la definición duplicada de `deleteFileIfExists` de este archivo.
//       La función ya está definida en commonMain/FileStorageManager.kt como
//       extension function. Tenerla aquí también causa compilación redundante.
