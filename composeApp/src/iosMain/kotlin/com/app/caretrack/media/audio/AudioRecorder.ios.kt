package com.app.caretrack.media.audio

import android.media.MediaRecorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.io.IOException

actual class AudioRecorder actual constructor(context: Any?) {
    private val androidContext = context as? android.content.Context
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null

    actual fun startRecording(path: String) {
        try {
            // Release any existing recorder
            mediaRecorder?.release()

            // Create new MediaRecorder
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(path)
                prepare()
            }

            // Start recording
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
