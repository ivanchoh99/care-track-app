package com.app.caretrack.media.file

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

// =============================================================================
// GESTIÓN DE ARCHIVOS — INTERFAZ MULTIPLATAFORMA (expect)
// =============================================================================
// El sistema de archivos también difiere entre plataformas:
//   Android → El directorio de la app es `context.filesDir` (privado, sin permisos)
//   iOS     → Se usan los directorios del sandbox de la app vía Foundation.FileManager
//
// Esta clase abstrae esas diferencias con el patrón expect/actual.
//
// Los archivos se guardan localmente para:
//   1. Mostrarlos en la UI sin conexión a internet.
//   2. Enviar la ruta al servidor en lugar de los bytes completos.
//   3. Reproducir audios grabados por el usuario.
// =============================================================================

/**
 * Gestiona el almacenamiento de archivos multimedia en el dispositivo.
 *
 * @param context En Android: el `android.content.Context`.
 *                En iOS: no se usa (null).
 */
expect class FileStorageManager(context: Any?) {

    /**
     * Guarda un archivo en el directorio privado de la app.
     *
     * En Android guarda en: `/data/data/com.app.caretrack/files/media/`
     * El directorio `media/` se crea automáticamente si no existe.
     *
     * @param bytes    Contenido del archivo como array de bytes.
     * @param fileName Nombre con el que se guardará el archivo.
     * @return Ruta absoluta del archivo guardado, ej:
     *         `/data/data/com.app.caretrack/files/media/foto.jpg`
     */
    fun saveFile(bytes: ByteArray, fileName: String): String

    /**
     * Elimina un archivo del sistema de archivos.
     *
     * @param path Ruta absoluta del archivo a eliminar.
     * @return `true` si se eliminó exitosamente, `false` si hubo error.
     */
    fun deleteFile(path: String): Boolean
}

/**
 * Extension function: elimina un archivo solo si la ruta no es null ni vacía.
 *
 * Una `extension function` en Kotlin agrega métodos a clases existentes
 * sin necesidad de modificarlas ni heredar de ellas.
 *
 * `suspend` → Esta función puede suspenderse (es una coroutine). El `withContext`
 * mueve la ejecución al hilo IO, liberando el hilo principal de la UI.
 *
 * Uso:
 * ```kotlin
 * fileStorageManager.deleteFileIfExists(message.filePath)
 * ```
 *
 * @param path Ruta del archivo. Si es null o vacía, no hace nada.
 * @return `true` si se eliminó, `false` en cualquier otro caso.
 */
suspend fun FileStorageManager.deleteFileIfExists(path: String?): Boolean {
    // La función termina aquí si el path es null o en blanco (early return)
    if (path.isNullOrBlank()) return false
    // withContext cambia el dispatcher al hilo IO para no bloquear la UI
    return withContext(Dispatchers.IO) {
        deleteFile(path)
    }
}

// TODO: La implementación iOS (FileStorageManager.ios.kt) actualmente no hace
//       nada (devuelve string vacío). Debe implementarse con Foundation.FileManager
//       para guardar archivos en el directorio Documents o Caches de la app iOS.
//
// TODO: Agregar un método `getFileSize(path: String): Long` y usarlo en
//       ChatRepository para calcular y guardar el campo `size` de MessageEntity.
//       Actualmente ese campo siempre es null.
//
// TODO: Implementar una política de limpieza de archivos: los archivos de mensajes
//       eliminados deberían borrarse del disco. Actualmente solo se borran si
//       se llama a deleteMessage() en el repositorio, pero si la app se reinstala
//       o el usuario borra datos, los archivos huérfanos pueden acumularse.
//
// TODO: Considerar comprimir imágenes antes de guardarlas para ahorrar espacio.
//       Una foto de 5MB no necesita guardarse a máxima resolución para mostrarse
//       en una burbuja de chat de 200dp.
