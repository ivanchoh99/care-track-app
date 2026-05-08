package com.app.caretrack.chat

import android.media.MediaRecorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.File

actual class AudioRecorder actual constructor(context: Any?) {
    private val androidContext = context as? android.content.Context
    private var recorder: MediaRecorder? = null

    // NUEVO: Variable para almacenar el nombre del archivo en curso
    private var currentRecordedPath: String = ""

    actual fun startRecording(path: String) {
        // Guardamos el nombre del archivo para usarlo al detener la grabación
        currentRecordedPath = path

        // Si el path no es absoluto, lo guardamos en la carpeta interna de la app
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

    // NUEVO: Cambiamos la función para que retorne un String
    actual fun stopRecording(): String {
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            println("CARETRACK_ERROR: Error al detener la grabación -> ${e.message}")
        } finally {
            recorder = null
        }

        // Validación de depuración (opcional)
        val file = File(androidContext?.filesDir, currentRecordedPath)
        if (file.exists()) {
            println("Validación: Audio guardado correctamente. Tamaño: ${file.length()} bytes en ${file.absolutePath}")
        }

        // Devolvemos la ruta/nombre que guardamos al iniciar
        return currentRecordedPath
    }
}

@Composable
actual fun rememberAudioRecorder(): AudioRecorder {
    val context = LocalContext.current
    return remember { AudioRecorder(context) }
}