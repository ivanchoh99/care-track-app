package com.app.caretrack.chat

import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.File

actual class AudioPlayer actual constructor(private val context: Any?) {
    private var mediaPlayer: MediaPlayer? = null
    private val androidContext = context as android.content.Context

    actual fun playAudio(path: String) {
        stopAudio()

        if (path.isBlank()) {
            AppLogger.e("AudioPlayer", "La ruta del audio está vacía.")
            return
        }
        try {
            val file = if (path.startsWith("/")) {
                File(path)
            } else {
                File(androidContext.filesDir, path)
            }

            if (file.isDirectory) {
                AppLogger.e("AudioPlayer", "Se intentó reproducir un directorio -> ${file.absolutePath}")
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
                AppLogger.e("AudioPlayer", "Archivo de audio no existe -> ${file.absolutePath}")
            }

            mediaPlayer?.setOnErrorListener { _, what, extra ->
                AppLogger.e("AudioPlayer", "Error nativo MediaPlayer -> what:$what extra:$extra")
                true
            }
        } catch (e: Exception) {
            AppLogger.e("AudioPlayer", "Excepción en playAudio -> ${e.message}")
        }
    }

    actual fun playAudioFromBytes(bytes: ByteArray) {
        stopAudio()
        try {
            val tempFile = File(androidContext.cacheDir, "caretrack_audio_cache.ogg")
            tempFile.writeBytes(bytes)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                prepare()
                start()
                setOnCompletionListener { stopAudio() }

                setOnErrorListener { _, what, extra ->
                    AppLogger.e("AudioPlayer", "Error reproduciendo bytes -> what:$what extra:$extra")
                    true
                }
            }
        } catch (e: Exception) {
            AppLogger.e("AudioPlayer", "Excepción reproduciendo bytes -> ${e.message}")
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
