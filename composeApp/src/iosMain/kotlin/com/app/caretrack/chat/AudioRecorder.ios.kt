package com.app.caretrack.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

actual class AudioRecorder actual constructor(context: Any?) {
    actual fun startRecording(path: String) {
        // Próximamente: Implementación con AVAudioRecorder
    }

    actual fun stopRecording() {
        // Próximamente
    }
}

@Composable
actual fun rememberAudioRecorder(): AudioRecorder {
    return remember { AudioRecorder(null) }
}