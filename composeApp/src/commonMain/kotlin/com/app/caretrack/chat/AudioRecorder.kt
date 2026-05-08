package com.app.caretrack.chat

import androidx.compose.runtime.Composable

// Añadimos un parámetro opcional para que Android pueda pasar su Context
expect class AudioRecorder(context: Any? = null) {
    fun startRecording(path: String)
    fun stopRecording()
}

@Composable
expect fun rememberAudioRecorder(): AudioRecorder