package com.app.caretrack.chat

import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.File

actual class AudioPlayer actual constructor(private val context: Any?) {
    private var mediaPlayer: MediaPlayer? = null
    private val androidContext = context as android.content.Context

    // 1. NOTAS DE VOZ (Restaurado a la versión directa que te funcionaba perfecto)
    actual fun playAudio(path: String) {
        stopAudio()

        if (path.isBlank()) {
            println("CARETRACK_ERROR: La ruta del audio está vacía.")
            return
        }
        try {
            // Si la ruta ya es absoluta (ej. /data/user/0/...), la usamos.
            // Si es solo el nombre (ej. nota.m4a), le agregamos la carpeta filesDir.
            val file = if (path.startsWith("/")) {
                File(path)
            } else {
                File(androidContext.filesDir, path)
            }

            // Segunda protección: Validar que no sea un directorio
            if (file.isDirectory) {
                println("CARETRACK_ERROR: Se intentó reproducir un directorio -> ${file.absolutePath}")
                return
            }

            if (file.exists()) {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(file.absolutePath)
                    prepare()
                    start()
                    setOnCompletionListener { stopAudio() }
                }
            } else {
                println("CARETRACK_ERROR: Archivo de audio no existe -> ${file.absolutePath}")
            }

            mediaPlayer?.setOnErrorListener { _, what, extra ->
                println("CARETRACK_ERROR: Error nativo MediaPlayer -> what:$what extra:$extra")
                true
            }
        } catch (e: Exception) {
            println("CARETRACK_ERROR: Excepción en playAudio -> ${e.message}")
        }
    }

    // 2. AUDIOS CARGADOS (Usamos tu rescate de bytes)
    actual fun playAudioFromBytes(bytes: ByteArray) {
        stopAudio()
        try {
            // Creamos un archivo temporal QUE NOSOTROS CONTROLAMOS y Android no borrará
            // Usamos .ogg porque es el formato contenedor que usa WhatsApp para los .opus
            val tempFile = File(androidContext.cacheDir, "caretrack_audio_cache.ogg")
            tempFile.writeBytes(bytes)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                prepare()
                start()
                setOnCompletionListener { stopAudio() }

                setOnErrorListener { _, what, extra ->
                    println("CARETRACK_ERROR: Error reproduciendo bytes -> what:$what extra:$extra")
                    true
                }
            }
        } catch (e: Exception) {
            println("CARETRACK_ERROR: Excepción reproduciendo bytes -> ${e.message}")
            e.printStackTrace()
        }
    }

    actual fun stopAudio() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            // Ignorar errores si ya estaba liberado
        } finally {
            mediaPlayer = null
        }
    }

    actual fun getDuration(): Int = mediaPlayer?.duration ?: 0
    actual fun getCurrentPosition(): Int = mediaPlayer?.currentPosition ?: 0
    actual fun isPlaying(): Boolean = mediaPlayer?.isPlaying ?: false
}

@Composable
actual fun rememberAudioPlayer(): AudioPlayer {
    val context = LocalContext.current
    return remember { AudioPlayer(context) }
}