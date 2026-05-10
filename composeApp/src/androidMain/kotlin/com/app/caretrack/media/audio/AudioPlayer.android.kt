package com.app.caretrack.media.audio

import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.app.caretrack.common.AppLogger
import java.io.File

// =============================================================================
// IMPLEMENTACIÓN ANDROID DEL REPRODUCTOR DE AUDIO (actual)
// =============================================================================
// Android provee `MediaPlayer` para reproducir audio y video.
// Su ciclo de vida tiene varios estados: Idle → Initialized → Prepared →
// Started → Paused → Stopped → End → Error.
//
// Es crítico seguir este ciclo correctamente o se lanzan excepciones.
// Por eso usamos `prepareAsync()` (no bloquea el hilo) y esperamos el
// callback `setOnPreparedListener` antes de llamar `start()`.
//
// Diagrama simplificado del ciclo de MediaPlayer:
//   new MediaPlayer()
//       ↓
//   setDataSource(path)   ← define qué archivo reproducir
//       ↓
//   prepareAsync()        ← carga el archivo en segundo plano
//       ↓ (callback)
//   setOnPreparedListener → start()  ← inicia la reproducción
//       ↓
//   stopAudio() → release()          ← libera recursos de memoria/hardware
// =============================================================================

/**
 * Implementación Android de [AudioPlayer] usando `android.media.MediaPlayer`.
 *
 * @param context Contexto Android necesario para acceder a `filesDir`.
 */
actual class AudioPlayer actual constructor(context: Any?) {
    private var mediaPlayer: MediaPlayer? = null
    // Cast seguro: si context no es Context de Android, androidContext queda null
    private val androidContext = context as? android.content.Context

    /**
     * Reproduce audio desde una ruta local o URL remota.
     *
     * La lógica de resolución de ruta:
     * 1. Si el path empieza con "/" → es una ruta absoluta, úsala directamente.
     * 2. Si no → es relativa al `filesDir`, se construye la ruta completa.
     *
     * Llama a `stopAudio()` primero para liberar cualquier MediaPlayer anterior
     * antes de crear uno nuevo (evita fugas de recursos).
     *
     * @param path Ruta absoluta del archivo o URL remota del audio.
     */
    actual fun playAudio(path: String) {
        stopAudio()  // Siempre detener el audio anterior antes de reproducir uno nuevo

        if (path.isBlank()) {
            AppLogger.e("AudioPlayer", "Audio path is empty.")
            return
        }

        AppLogger.d("AudioPlayer", "Intentando reproducir audio - path: $path")

        try {
            // Resolver ruta: absoluta vs relativa a filesDir
            val file = if (path.startsWith("/")) {
                File(path)
            } else {
                File(androidContext?.filesDir, path)
            }

            AppLogger.d("AudioPlayer", "Archivo resolved path: ${file.absolutePath}")
            AppLogger.d("AudioPlayer", "Archivo existe: ${file.exists()}, tamaño: ${file.length()} bytes")

            // Verificar que no sea un directorio (protección contra bugs de ruta)
            if (file.isDirectory) {
                AppLogger.e("AudioPlayer", "Attempted to play a directory -> ${file.absolutePath}")
                return
            }

            if (file.exists()) {
                // Crear MediaPlayer con el patrón apply{} de Kotlin:
                // ejecuta el bloque sobre el objeto recién creado y devuelve el objeto.
                // Es equivalente a: val mp = MediaPlayer(); mp.setDataSource(...); ...
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(file.absolutePath)

                    // prepareAsync() carga el archivo en un hilo de fondo.
                    // Cuando está listo, llama a onPrepared. NUNCA bloquea la UI.
                    setOnPreparedListener {
                        AppLogger.d("AudioPlayer", "MediaPlayer preparado, iniciando reproducción")
                        start()  // Inicia la reproducción cuando el archivo está listo
                    }

                    // Se llama automáticamente cuando el audio termina
                    setOnCompletionListener {
                        AppLogger.d("AudioPlayer", "Reproducción completada")
                        stopAudio()  // Libera recursos al terminar
                    }

                    // Se llama si ocurre un error durante la reproducción
                    setOnErrorListener { _, what, extra ->
                        AppLogger.e("AudioPlayer", "MediaPlayer error - what:$what extra:$extra")
                        stopAudio()
                        true  // `true` indica que manejamos el error nosotros
                    }

                    prepareAsync()  // Inicia la carga asíncrona
                }
                AppLogger.d("AudioPlayer", "prepareAsync() iniciado para: ${file.absolutePath}")
            } else {
                AppLogger.e("AudioPlayer", "Audio file does not exist -> ${file.absolutePath}")
            }
        } catch (e: Exception) {
            AppLogger.e("AudioPlayer", "Exception in playAudio: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Reproduce audio directamente desde bytes (sin archivo previo).
     *
     * Escribe los bytes en un archivo temporal en `cacheDir` y lo reproduce.
     * El archivo en caché puede ser eliminado por el sistema cuando hay
     * poco espacio disponible, por lo que no es para almacenamiento persistente.
     *
     * @param bytes Contenido binario del archivo de audio.
     */
    actual fun playAudioFromBytes(bytes: ByteArray) {
        stopAudio()
        try {
            // cacheDir es el directorio de caché de la app: /data/data/.../cache/
            // Los archivos aquí son temporales y el sistema puede borrarlos
            val tempFile = File(androidContext?.cacheDir, "caretrack_audio_cache.ogg")
            tempFile.writeBytes(bytes)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                setOnPreparedListener { start() }
                setOnCompletionListener { stopAudio() }
                setOnErrorListener { _, what, extra ->
                    AppLogger.e("AudioPlayer", "Error playing bytes -> what:$what extra:$extra")
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            AppLogger.e("AudioPlayer", "Exception playing bytes -> ${e.message}")
        }
    }

    /**
     * Detiene la reproducción y libera todos los recursos del MediaPlayer.
     *
     * `release()` es crucial: sin él, el MediaPlayer sigue ocupando memoria
     * de audio del sistema (un recurso hardware limitado). En Android, no
     * liberar el MediaPlayer puede impedir que otras apps usen el audio.
     *
     * El bloque `try/finally` garantiza que `mediaPlayer = null` se ejecute
     * incluso si `release()` lanza una excepción.
     */
    actual fun stopAudio() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()  // ← SIEMPRE llamar release() para liberar recursos de audio
            }
        } catch (_: Exception) {
            // Ignorar excepciones al detener (puede ocurrir si el MediaPlayer
            // está en un estado inválido, ej. si se interrumpió la preparación)
        } finally {
            mediaPlayer = null  // Siempre limpiar la referencia para que el GC libere memoria
        }
    }

    // Devuelven 0 si no hay MediaPlayer activo (el operador `?.` hace el null-check)
    actual fun getDuration(): Int = mediaPlayer?.duration ?: 0
    actual fun getCurrentPosition(): Int = mediaPlayer?.currentPosition ?: 0
    actual fun isPlaying(): Boolean = mediaPlayer?.isPlaying ?: false
}

/**
 * Crea y recuerda un [AudioPlayer] ligado al ciclo de vida del Composable.
 *
 * `LocalContext.current` es una forma de acceder al Context de Android
 * desde dentro de un Composable sin tener que pasarlo como parámetro.
 * Es un "CompositionLocal" — un valor que se propaga implícitamente por
 * el árbol de Composables.
 *
 * `remember { ... }` garantiza que el AudioPlayer se crea solo una vez
 * y se reutiliza en cada recomposición de la pantalla.
 */
@Composable
actual fun rememberAudioPlayer(): AudioPlayer {
    val context = LocalContext.current
    return remember { AudioPlayer(context) }
}

// TODO: Implementar `pause()` y `resume()` separados de `stopAudio()`.
//       Actualmente al "pausar" el audio se pierde la posición y empieza
//       desde el principio al volver a reproducir.
//
// TODO: Agregar `seekTo(positionMs: Int)` para que el usuario pueda saltar
//       a cualquier punto del audio arrastrando la barra de progreso.
//
// TODO: El método `playAudioFromBytes` siempre usa el mismo nombre de archivo
//       temporal ("caretrack_audio_cache.ogg"). Si se reproducen dos audios
//       simultáneamente (edge case), el segundo sobreescribiría el primero.
//       Usar un nombre único por reproducción.
//
// TODO: Agregar lógica para reanudar el audio cuando una llamada interrumpe
//       la reproducción (Audio Focus en Android). Actualmente el audio
//       compite con llamadas y otras apps sin ceder el foco correctamente.
