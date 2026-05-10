package com.app.caretrack.chat

import android.media.MediaRecorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.File

actual class AudioRecorder actual constructor(context: Any?) {
    private val androidContext = context as? android.content.Context
    private var recorder: MediaRecorder? = null
    private var currentRecordedPath: String = ""
    private var lastAbsolutePath: String = ""

    @Suppress("DEPRECATION")
    actual fun startRecording(path: String) {
        currentRecordedPath = path
        val absolutePath = if (path.startsWith("/")) {
            path
        } else {
            File(androidContext?.filesDir, path).absolutePath
        }
        lastAbsolutePath = absolutePath

        recorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            androidContext?.let { MediaRecorder(it) } ?: MediaRecorder()
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(absolutePath)
            prepare()
            start()
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
