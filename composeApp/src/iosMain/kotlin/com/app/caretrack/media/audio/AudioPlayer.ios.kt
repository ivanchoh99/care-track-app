package com.app.caretrack.media.audio

import androidx.compose.runtime.Composable
import android.media.MediaPlayer  // ⚠️ INCORRECTO: Este import no existe en iOS
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.File

// =============================================================================
// IMPLEMENTACIÓN iOS DEL REPRODUCTOR DE AUDIO — STUB INCORRECTO
// =============================================================================
// ⚠️ ADVERTENCIA CRÍTICA: Este archivo usa imports de Android (`android.media.MediaPlayer`,
// `java.io.File`, `androidx.compose.ui.platform.LocalContext`) que NO existen
// en el entorno iOS de Kotlin/Native.
//
// Este archivo no compilará para iOS. Es una copia del código Android
// que necesita ser completamente reescrita para iOS.
//
// La implementación iOS correcta usaría AVAudioPlayer de AVFoundation:
// ```kotlin
// import platform.AVFoundation.AVAudioPlayer
// import platform.Foundation.NSURL
//
// actual class AudioPlayer actual constructor(private val context: Any?) {
//     private var avPlayer: AVAudioPlayer? = null
//
//     actual fun playAudio(path: String) {
//         val url = NSURL.fileURLWithPath(path)
//         avPlayer = AVAudioPlayer(contentsOfURL = url, error = null)
//         avPlayer?.play()
//     }
//     // ...
// }
// ```
// =============================================================================

/**
 * ⚠️ STUB INCORRECTO — Implementación iOS de [AudioPlayer].
 *
 * Este código no compilará en iOS porque usa APIs de Android.
 * Ver los TODOs al final del archivo para la implementación correcta.
 */
actual class AudioPlayer actual constructor(private val context: Any?) {
    private var mediaPlayer: MediaPlayer? = null

    init {
        if (context != null) {
            val ctx = context as? android.content.Context
            if (ctx != null) {
                // Initialize MediaPlayer if needed
            }
        }
    }

    actual fun playAudio(path: String) {
        stopAudio()
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(path)
                prepare()
                start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    actual fun playAudioFromBytes(bytes: ByteArray) {
        stopAudio()
        val tempFile = File.createTempFile("audio_temp", ".mp3", null)
        tempFile.outputStream().use { it.write(bytes) }
        tempFile.deleteOnExit()
        playAudio(tempFile.absolutePath)
    }

    actual fun stopAudio() {
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null
    }

    actual fun getDuration(): Int = mediaPlayer?.duration?.toInt() ?: 0
    actual fun getCurrentPosition(): Int = mediaPlayer?.currentPosition?.toInt() ?: 0
    actual fun isPlaying(): Boolean = mediaPlayer?.isPlaying ?: false

    @Composable
    actual fun rememberAudioPlayer(): AudioPlayer {
        return remember(context) { AudioPlayer(context) }
    }
}

// TODO: CRÍTICO — Reescribir completamente este archivo para iOS usando AVFoundation:
//       import platform.AVFoundation.AVAudioPlayer
//       import platform.Foundation.NSURL
//       Eliminar todos los imports de android.* y java.* que no existen en iOS.
//
// TODO: La función `rememberAudioPlayer()` dentro de la clase es incorrecta
//       (los Composables no van dentro de clases). Debe ser una función de nivel
//       superior (top-level function) en todas las plataformas. Revisar también
//       la implementación Android.
//
// TODO: En iOS, AVAudioPlayer NO soporta URLs remotas directamente.
//       Para audio remoto se necesita AVPlayer (distinto de AVAudioPlayer).
//       Implementar lógica para elegir entre AVAudioPlayer (local) y AVPlayer (remoto).
