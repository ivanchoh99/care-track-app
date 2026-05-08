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
        try {
            mediaPlayer = MediaPlayer().apply {
                val file = File(androidContext.filesDir, path)

                if (file.exists()) {
                    setDataSource(file.absolutePath)
                    prepare()
                    start()
                    setOnCompletionListener { stopAudio() }
                } else {
                    println("CARETRACK_ERROR: Nota de voz no existe -> ${file.absolutePath}")
                }

                setOnErrorListener { _, what, extra ->
                    println("CARETRACK_ERROR: Error nativo MediaPlayer -> what:$what extra:$extra")
                    true
                }
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