package com.app.caretrack.media.audio

import android.media.MediaRecorder  // ⚠️ INCORRECTO: No existe en iOS
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.io.IOException

// =============================================================================
// IMPLEMENTACIÓN iOS DEL GRABADOR DE AUDIO — STUB INCORRECTO
// =============================================================================
// ⚠️ ADVERTENCIA CRÍTICA: Al igual que AudioPlayer.ios.kt, este archivo usa
// imports de Android que NO existen en iOS (android.media.MediaRecorder,
// java.io.File, java.io.IOException).
//
// La implementación iOS correcta usaría AVAudioRecorder de AVFoundation:
// ```kotlin
// import platform.AVFoundation.AVAudioRecorder
// import platform.AVFoundation.AVAudioSession
// import platform.Foundation.NSURL
// import platform.Foundation.NSDocumentDirectory
//
// actual class AudioRecorder actual constructor(context: Any? = null) {
//     private var recorder: AVAudioRecorder? = null
//
//     actual fun startRecording(path: String) {
//         val url = NSURL.fileURLWithPath(path)
//         val settings = mapOf(
//             AVFormatIDKey to kAudioFormatMPEG4AAC,
//             AVSampleRateKey to 44100.0,
//             AVNumberOfChannelsKey to 1
//         )
//         recorder = AVAudioRecorder(URL = url, settings = settings, error = null)
//         recorder?.record()
//     }
//     // ...
// }
// ```
// =============================================================================

/**
 * ⚠️ STUB INCORRECTO — Implementación iOS de [AudioRecorder].
 *
 * Este código usa APIs de Android y no compilará para iOS.
 * Debe reemplazarse con una implementación que use AVAudioRecorder.
 */
actual class AudioRecorder actual constructor(context: Any?) {
    private val androidContext = context as? android.content.Context
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null

    actual fun startRecording(path: String) {
        try {
            mediaRecorder?.release()
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(path)
                prepare()
            }
            mediaRecorder?.start()
            outputFile = File(path)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    actual fun stopRecording(): String {
        mediaRecorder?.let {
            it.stop()
            it.release()
        }
        mediaRecorder = null
        return outputFile?.absolutePath ?: ""
    }

    @Composable
    actual fun rememberAudioRecorder(): AudioRecorder {
        return remember(context) { AudioRecorder(context) }
    }
}

// TODO: CRÍTICO — Reescribir con AVAudioRecorder de AVFoundation para iOS.
//       Eliminar todos los imports de android.* y java.* que no existen en iOS.
//
// TODO: En iOS, se debe activar la sesión de audio antes de grabar:
//       AVAudioSession.sharedInstance().setCategory(.record, mode: .default)
//       AVAudioSession.sharedInstance().setActive(true)
//       Sin esto, la grabación fallará en dispositivos reales.
//
// TODO: Al igual que en AudioPlayer.ios.kt, la función `rememberAudioRecorder()`
//       no debe estar dentro de la clase. Moverla a función top-level.
