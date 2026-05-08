package com.app.caretrack.chat

import android.media.MediaRecorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.File

actual class AudioRecorder actual constructor(context: Any?) {
    private val androidContext = context as? android.content.Context
    private var recorder: MediaRecorder? = null

    actual fun startRecording(path: String) {
        // SOLUCIÓN: Si el path no es absoluto, lo guardamos en la carpeta interna de la app
        val absolutePath = if (path.startsWith("/")) {
            path
        } else {
            File(androidContext?.filesDir, path).absolutePath
        }

        recorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            androidContext?.let { MediaRecorder(it) } ?: MediaRecorder()
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(absolutePath) // Usamos la ruta corregida
            prepare()
            start()
        }
    }

    actual fun stopRecording() {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null

        // Validación de depuración: Comprobar si el archivo existe y tiene tamaño
        val file = File(androidContext?.filesDir, "last_audio.m4a")
        if (file.exists()) {
            println("Validación: Audio guardado correctamente. Tamaño: ${file.length()} bytes")
        }
    }
}

@Composable
actual fun rememberAudioRecorder(): AudioRecorder {
    val context = LocalContext.current
    return remember { AudioRecorder(context) }
}