package com.app.caretrack.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSError
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSUserDomainMask

actual class AudioPlayer actual constructor(context: Any?) {
    private var player: AVAudioPlayer? = null

    actual fun playAudio(path: String) {
        stopAudio()
        if (path.isBlank()) return

        try {
            val url = if (path.startsWith("/")) {
                NSURL.fileURLWithPath(path)
            } else {
                val documentsDir = NSSearchPathForDirectoriesInDomains(
                    NSDocumentDirectory, NSUserDomainMask, true
                ).first() as String
                NSURL.fileURLWithPath("$documentsDir/$path")
            }

            val error = NSError()
            player = AVAudioPlayer(contentsOfURL = url, error = error)
            player?.prepareToPlay()
            player?.play()
        } catch (e: Exception) {
            println("CARETRACK_ERROR: Excepción en playAudio -> ${e.message}")
        }
    }

    actual fun playAudioFromBytes(bytes: ByteArray) {
        stopAudio()
        try {
            val tempDir = NSTemporaryDirectory() ?: ""
            val tempPath = "${tempDir}caretrack_audio_cache.ogg"
            val data = NSData.create(bytes = bytes, length = bytes.size.toULong())
            data?.writeToFile(tempPath, atomically = true)

            val url = NSURL.fileURLWithPath(tempPath)
            val error = NSError()
            player = AVAudioPlayer(contentsOfURL = url, error = error)
            player?.prepareToPlay()
            player?.play()
        } catch (e: Exception) {
            println("CARETRACK_ERROR: Excepción en playAudioFromBytes -> ${e.message}")
        }
    }

    actual fun stopAudio() {
        try {
            player?.stop()
            player = null
        } catch (_: Exception) {
        }
    }

    actual fun getDuration(): Int = ((player?.duration ?: 0.0) * 1000).toInt()
    actual fun getCurrentPosition(): Int = ((player?.currentTime ?: 0.0) * 1000).toInt()
    actual fun isPlaying(): Boolean = player?.playing ?: false
}

@Composable
actual fun rememberAudioPlayer(): AudioPlayer {
    return remember { AudioPlayer(null) }
}
