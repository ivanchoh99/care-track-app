package com.app.caretrack.media.audio

import android.content.Context
import android.media.MediaRecorder
import com.app.caretrack.common.AppLogger
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.File

actual class AudioRecorder actual constructor(context: Any?) {
    private var androidContext: Context? = null
    
    init {
        // Verificar y guardar el contexto
        androidContext = if (context is Context) {
            context.applicationContext as? Context
        } else {
            context as? Context
        }
        AppLogger.d("AudioRecorder", "AudioRecorder inicializado con contexto: ${androidContext?.filesDir}")
    }
    
    private var recorder: MediaRecorder? = null
    private var currentRecordedPath: String = ""
    private var lastAbsolutePath: String = ""

    @Suppress("DEPRECATION")
    actual fun startRecording(path: String) {
        // LIBERAR recorder anterior para evitar conflictos de estado
        try {
            recorder?.release()
        } catch (_: Exception) { }
        recorder = null
        
        currentRecordedPath = path
        
        // Asegurar que el directorio existe
        val filesDir = androidContext?.filesDir
        if (filesDir == null) {
            AppLogger.e("AudioRecorder", "Error: contexto de aplicación no disponible")
            throw IllegalStateException("Application context not available")
        }
        
        val absolutePath = if (path.startsWith("/")) {
            path
        } else {
            File(filesDir, path).absolutePath
        }
        lastAbsolutePath = absolutePath
        
        // Crear directorio si no existe
        File(absolutePath).parentFile?.mkdirs()

        AppLogger.d("AudioRecorder", "Intentando grabar en: $absolutePath")
        AppLogger.d("AudioRecorder", "FilesDir: ${filesDir.absolutePath}")

        try {
            recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(absolutePath)
                prepare()
                start()
            }
            AppLogger.d("AudioRecorder", "✅ Grabación iniciada exitosamente - path: $absolutePath")
        } catch (e: Exception) {
            AppLogger.e("AudioRecorder", "❌ Error al iniciar grabación: ${e.message}")
            AppLogger.e("AudioRecorder", "Stack trace: ${e.stackTraceToString()}")
            try { recorder?.release() } catch (_: Exception) { }
            recorder = null
            throw e
        }
    }

    actual fun stopRecording(): String {
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            AppLogger.e("AudioRecorder", "Error al detener la grabación -> ${e.message}")
        } finally {
            recorder = null
        }

        val file = File(lastAbsolutePath)
        if (file.exists()) {
            AppLogger.d("AudioRecorder", "Audio guardado. Tamaño: ${file.length()} bytes en ${file.absolutePath}")
        }
        return lastAbsolutePath
    }
}

@Composable
actual fun rememberAudioRecorder(): AudioRecorder {
    val context = LocalContext.current
    return remember { AudioRecorder(context) }
}
