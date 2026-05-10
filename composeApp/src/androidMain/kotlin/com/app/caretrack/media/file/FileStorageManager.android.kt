package com.app.caretrack.media.file

import android.content.Context
import com.app.caretrack.common.AppLogger
import java.io.File

// =============================================================================
// IMPLEMENTACIÓN ANDROID DE FileStorageManager (actual)
// =============================================================================
// En Android, los archivos de la app se guardan en el directorio privado:
//   /data/data/com.app.caretrack/files/
//
// Este directorio es privado — otras apps no pueden accederlo sin root.
// No requiere el permiso WRITE_EXTERNAL_STORAGE (solo necesario para la
// tarjeta SD, que está en desuso desde Android 10+).
//
// Los archivos multimedia se guardan en el subdirectorio /media/:
//   /data/data/com.app.caretrack/files/media/foto.jpg
//   /data/data/com.app.caretrack/files/media/audio.wav
// =============================================================================

/**
 * Implementación Android de [FileStorageManager].
 *
 * Usa `applicationContext` (en lugar del Context de la Activity) para evitar
 * memory leaks: si guardáramos el Context de la Activity, esta no podría ser
 * liberada por el GC mientras FileStorageManager esté vivo.
 *
 * @param context El contexto de Android. Se convierte a `applicationContext`
 *                para evitar retener la Activity en memoria.
 */
actual class FileStorageManager actual constructor(context: Any?) {
    // Cast seguro: `as?` devuelve null si el cast falla (en lugar de lanzar excepción)
    // `.applicationContext` garantiza que no retenemos la Activity
    private val appContext = (context as? Context)?.applicationContext

    /**
     * Guarda los bytes en un archivo dentro de `filesDir/media/`.
     *
     * - Crea el directorio `media/` si no existe.
     * - Si ya existe un archivo con ese nombre, lo sobreescribe.
     *
     * @param bytes    Contenido binario del archivo.
     * @param fileName Nombre del archivo a guardar.
     * @return Ruta absoluta del archivo guardado.
     */
    actual fun saveFile(bytes: ByteArray, fileName: String): String {
        // filesDir es el directorio privado de la app en el sistema de archivos
        val dir = File(appContext?.filesDir, "media")
        // mkdirs() crea el directorio y todos sus padres si no existen
        if (!dir.exists()) {
            dir.mkdirs()
        }

        val file = File(dir, fileName)
        // writeBytes() sobrescribe el archivo si ya existe
        file.writeBytes(bytes)
        return file.absolutePath  // ruta absoluta, ej: /data/data/.../files/media/foto.jpg
    }

    /**
     * Elimina el archivo en la ruta especificada.
     *
     * @param path Ruta absoluta del archivo.
     * @return `true` si se eliminó, `false` si no existía o hubo error.
     */
    actual fun deleteFile(path: String): Boolean {
        return try {
            File(path).delete()
        } catch (e: Exception) {
            AppLogger.e("FileStorage", "Error deleting file: ${e.message}")
            false
        }
    }
}

// TODO: Agregar un método `getFileSize(path: String): Long` que devuelva
//       `File(path).length()`. Úsarlo en ChatRepository al guardar archivos
//       para calcular el campo `size` de MessageEntity en formato legible
//       (ej. "1.2 MB"). Actualmente ese campo siempre es null.
//
// TODO: Manejar el caso donde `appContext` es null (si se pasa null como context).
//       Actualmente causaría un NullPointerException al llamar a `appContext?.filesDir`.
//       Agregar una verificación o lanzar IllegalStateException con un mensaje claro.
//
// TODO: Considerar guardar los archivos con un nombre único (UUID) en lugar del
//       nombre original, para evitar colisiones cuando dos archivos tienen el
//       mismo nombre. El nombre original puede guardarse en MessageEntity.fileName.
