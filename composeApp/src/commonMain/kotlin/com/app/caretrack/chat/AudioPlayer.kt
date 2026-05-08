// Ubicación: composeApp/src/commonMain/kotlin/com/app/caretrack/chat/AudioPlayer.kt
package com.app.caretrack.chat

import androidx.compose.runtime.Composable

expect class AudioPlayer(context: Any?) {
    fun playAudio(path: String)

    fun playAudioFromBytes(bytes: ByteArray)

    fun stopAudio()

    fun getDuration(): Int
    fun getCurrentPosition(): Int
    fun isPlaying(): Boolean
}

@Composable
expect fun rememberAudioPlayer(): AudioPlayer