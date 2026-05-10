package com.app.caretrack.media.audio

import androidx.compose.runtime.Composable
import android.media.MediaPlayer
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.File

actual class AudioPlayer actual constructor(private val context: Any?) {
    private var mediaPlayer: MediaPlayer? = null

    init {
        if (context != null) {
            val ctx = context as? android.content.Context
            if (ctx != null) {
                // Initialize MediaPlayer if needed
            }
        }
    }

    actual fun playAudio(path: String) {
        // Stop any currently playing audio
        stopAudio()

        // Create new MediaPlayer
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(path)
                prepare()
                start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    actual fun playAudioFromBytes(bytes: ByteArray) {
        // Stop any currently playing audio
        stopAudio()

        // Create temporary file from bytes
        val tempFile = File.createTempFile("audio_temp", ".mp3", null)
        tempFile.outputStream().use { it.write(bytes) }
        tempFile.deleteOnExit()

        // Play the temporary file
        playAudio(tempFile.absolutePath)
    }

    actual fun stopAudio() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
    }

    actual fun getDuration(): Int {
        return mediaPlayer?.duration?.toInt() ?: 0
    }

    actual fun getCurrentPosition(): Int {
        return mediaPlayer?.currentPosition?.toInt() ?: 0
    }

    actual fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying ?: false
    }

    @Composable
    actual fun rememberAudioPlayer(): AudioPlayer {
        return remember(context) { AudioPlayer(context) }
    }
}
