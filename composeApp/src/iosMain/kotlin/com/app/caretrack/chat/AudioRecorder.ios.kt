package com.app.caretrack.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

actual class AudioRecorder actual constructor(context: Any?) {
    private var currentPath: String = ""

    actual fun startRecording(path: String) {
        currentPath = path
        // TODO: Implementar con AVAudioRecorder
    }

    actual fun stopRecording(): String {
        // TODO: Implementar con AVAudioRecorder
        return currentPath
    }
}

@Composable
actual fun rememberAudioRecorder(): AudioRecorder {
    return remember { AudioRecorder(null) }
}