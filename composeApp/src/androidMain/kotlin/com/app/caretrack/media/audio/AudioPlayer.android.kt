package com.app.caretrack.media.audio

import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.app.caretrack.common.AppLogger
import java.io.File

actual class AudioPlayer actual constructor(context: Any?) {
    private var mediaPlayer: MediaPlayer? = null
    private val androidContext = context as? android.content.Context

    actual fun playAudio(path: String) {
        stopAudio()

        if (path.isBlank()) {
            AppLogger.e("AudioPlayer", "Audio path is empty.")
            return
        }
        
        AppLogger.d("AudioPlayer", "Intentando reproducir audio - path: $path")
        
        try {
            val file = if (path.startsWith("/")) {
                File(path)
            } else {
                File(androidContext?.filesDir, path)
            }

            AppLogger.d("AudioPlayer", "Archivo resolved path: ${file.absolutePath}")
            AppLogger.d("AudioPlayer", "Archivo existe: ${file.exists()}, tamaño: ${file.length()} bytes")

            if (file.isDirectory) {
                AppLogger.e("AudioPlayer", "Attempted to play a directory -> ${file.absolutePath}")
                return
            }

            if (file.exists()) {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(file.absolutePath)
                    // Usar prepareAsync() para no bloquear el hilo principal
                    setOnPreparedListener {
                        AppLogger.d("AudioPlayer", "MediaPlayer preparado, iniciando reproducción")
                        start()
                    }
                    setOnCompletionListener { 
                        AppLogger.d("AudioPlayer", "Reproducción completada")
                        stopAudio() 
                    }
                    setOnErrorListener { _, what, extra ->
                        AppLogger.e("AudioPlayer", "MediaPlayer error - what:$what extra:$extra")
                        stopAudio()
                        true
                    }
                    prepareAsync()  // No bloquea el hilo principal
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

    actual fun playAudioFromBytes(bytes: ByteArray) {
        stopAudio()
        try {
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
                prepareAsync()  // No bloquea el hilo principal
            }
        } catch (e: Exception) {
            AppLogger.e("AudioPlayer", "Exception playing bytes -> ${e.message}")
        }
    }

    actual fun stopAudio() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (_: Exception) {
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
