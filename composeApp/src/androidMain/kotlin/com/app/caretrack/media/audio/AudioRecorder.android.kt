package com.app.caretrack.media.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.app.caretrack.common.AppLogger
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile

actual class AudioRecorder actual constructor(context: Any?) {
    private var androidContext: Context? = null
    
    init {
        androidContext = if (context is Context) {
            context.applicationContext as? Context
        } else {
            context as? Context
        }
        AppLogger.d("AudioRecorder", "AudioRecorder inicializado con contexto: ${androidContext?.filesDir}")
    }
    
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var isRecording = false
    private var currentRecordedPath: String = ""
    private var lastAbsolutePath: String = ""
    
    companion object {
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    actual fun startRecording(path: String) {
        // Detener cualquier grabación anterior
        stopRecording()
        
        val filesDir = androidContext?.filesDir
        if (filesDir == null) {
            AppLogger.e("AudioRecorder", "Error: contexto de aplicación no disponible")
            throw IllegalStateException("Application context not available")
        }
        
        currentRecordedPath = path
        val absolutePath = if (path.startsWith("/")) {
            path
        } else {
            File(filesDir, path).absolutePath
        }
        lastAbsolutePath = absolutePath
        
        // Crear directorio si no existe
        File(absolutePath).parentFile?.mkdirs()
        
        // Eliminar archivo anterior si existe
        File(absolutePath).delete()
        
        AppLogger.d("AudioRecorder", "Iniciando grabación con AudioRecord - path: $absolutePath")
        
        try {
            val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            AppLogger.d("AudioRecorder", "Buffer size mínimo: $bufferSize")
            
            // Usar el contexto de la aplicación para AudioRecord
            val actualContext = androidContext ?: throw IllegalStateException("Contexto no disponible")
            
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize * 2
            )
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                AppLogger.e("AudioRecorder", "AudioRecord no se inicializó correctamente. Estado: ${audioRecord?.state}")
                throw IllegalStateException("AudioRecord no inicializado")
            }
            
            isRecording = true
            audioRecord?.startRecording()
            
            AppLogger.d("AudioRecorder", "✅ AudioRecord iniciado - state: ${audioRecord?.state}")
            
            // Iniciar hilo de grabación
            val scope = CoroutineScope(Dispatchers.IO)
            recordingJob = scope.launch {
                writeAudioDataToFile(absolutePath, bufferSize)
            }
            
        } catch (e: SecurityException) {
            AppLogger.e("AudioRecorder", "❌ Error de permisos: ${e.message}")
            throw e
        } catch (e: Exception) {
            AppLogger.e("AudioRecorder", "❌ Error al iniciar grabación: ${e.message}")
            AppLogger.e("AudioRecorder", "Stack: ${e.stackTraceToString()}")
            cleanup()
            throw e
        }
    }
    
    private suspend fun writeAudioDataToFile(filePath: String, bufferSize: Int) {
        val buffer = ByteArray(bufferSize)
        val outputStream = FileOutputStream(filePath)
        
        // Escribir header WAV placeholder (se actualizará al cerrar)
        val wavHeader = ByteArray(44)
        outputStream.write(wavHeader)
        
        var totalBytesWritten = 0L
        
        try {
            while (currentCoroutineContext().isActive && isRecording) {
                val read = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                if (read > 0) {
                    outputStream.write(buffer, 0, read)
                    totalBytesWritten += read
                }
            }
        } finally {
            outputStream.close()
            AppLogger.d("AudioRecorder", "Grabación terminada. Total bytes: $totalBytesWritten")
            
            // Actualizar header WAV con el tamaño real
            if (totalBytesWritten > 0) {
                updateWavHeader(filePath, totalBytesWritten)
            }
        }
    }
    
    private fun updateWavHeader(filePath: String, dataSize: Long) {
        try {
            val raf = RandomAccessFile(filePath, "rw")
            
            // RIFF header
            raf.seek(0)
            raf.writeBytes("RIFF")
            raf.write(intToByteArray((36 + dataSize).toInt()))
            raf.writeBytes("WAVE")
            
            // fmt subchunk
            raf.writeBytes("fmt ")
            raf.write(intToByteArray(16)) // Subchunk1Size for PCM
            raf.write(shortToByteArray(1)) // AudioFormat (1 = PCM)
            raf.write(shortToByteArray(1)) // NumChannels (mono)
            raf.write(intToByteArray(SAMPLE_RATE)) // SampleRate
            raf.write(intToByteArray(SAMPLE_RATE * 1 * 16 / 8)) // ByteRate
            raf.write(shortToByteArray(1 * 16 / 8)) // BlockAlign
            raf.write(shortToByteArray(16)) // BitsPerSample
            
            // data subchunk
            raf.writeBytes("data")
            raf.write(intToByteArray(dataSize.toInt()))
            
            raf.close()
            AppLogger.d("AudioRecorder", "Header WAV actualizado")
        } catch (e: Exception) {
            AppLogger.e("AudioRecorder", "Error actualizando header WAV: ${e.message}")
        }
    }
    
    private fun intToByteArray(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte()
        )
    }
    
    private fun shortToByteArray(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte()
        )
    }

    actual fun stopRecording(): String {
        AppLogger.d("AudioRecorder", "Deteniendo grabación...")
        
        isRecording = false
        recordingJob?.cancel()
        recordingJob = null
        
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            AppLogger.e("AudioRecorder", "Error al detener AudioRecord: ${e.message}")
        } finally {
            audioRecord = null
        }

        val file = File(lastAbsolutePath)
        if (file.exists() && file.length() > 0) {
            AppLogger.d("AudioRecorder", "✅ Audio guardado. Tamaño: ${file.length()} bytes en ${file.absolutePath}")
        } else {
            AppLogger.e("AudioRecorder", "Archivo no existe o está vacío: $lastAbsolutePath")
        }
        
        return lastAbsolutePath
    }
    
    private fun cleanup() {
        isRecording = false
        recordingJob?.cancel()
        recordingJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) { }
        audioRecord = null
    }
}

@Composable
actual fun rememberAudioRecorder(): AudioRecorder {
    val context = LocalContext.current
    return remember { AudioRecorder(context) }
}