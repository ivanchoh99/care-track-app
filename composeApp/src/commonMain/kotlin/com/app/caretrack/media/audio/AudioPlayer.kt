// Ubicación: composeApp/src/commonMain/kotlin/com/app/caretrack/media/audio/AudioPlayer.kt
package com.app.caretrack.media.audio

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