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

// =============================================================================
// IMPLEMENTACIÓN ANDROID DEL GRABADOR DE AUDIO (actual)
// =============================================================================
// Esta implementación usa `AudioRecord` de bajo nivel (PCM raw) en lugar
// de `MediaRecorder` de alto nivel.
//
// ¿Por qué AudioRecord en lugar de MediaRecorder?
// → AudioRecord da acceso directo a los bytes PCM del micrófono.
// → Permite controlar exactamente el formato de salida (WAV en este caso).
// → MediaRecorder encapsula el proceso pero tiene menos control y causó
//   problemas con algunos emuladores (ver historial de commits).
//
// El formato WAV es un contenedor de audio sin compresión:
//   [Header de 44 bytes] + [Datos PCM raw]
// El header describe parámetros como: tasa de muestreo, canales, bits por muestra.
// Como grabamos primero los datos y luego sabemos el tamaño, escribimos un
// header vacío al inicio y lo actualizamos al terminar con RandomAccessFile.
//
// Flujo de grabación:
//   1. startRecording() → inicializa AudioRecord + lanza coroutine de escritura
//   2. writeAudioDataToFile() → lee PCM del micrófono y escribe al archivo (loop)
//   3. stopRecording() → cancela la coroutine, detiene AudioRecord, completa WAV
// =============================================================================

/**
 * Implementación Android de [AudioRecorder] usando `AudioRecord` con salida WAV.
 */
actual class AudioRecorder actual constructor(context: Any?) {
    private var androidContext: Context? = null

    init {
        // Usamos `applicationContext` para no retener la Activity en memoria
        androidContext = if (context is Context) {
            context.applicationContext as? Context
        } else {
            context as? Context
        }
        AppLogger.d("AudioRecorder", "AudioRecorder inicializado con contexto: ${androidContext?.filesDir}")
    }

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null      // La coroutine de escritura al archivo
    private var isRecording = false            // Flag para controlar el loop de escritura
    private var currentRecordedPath: String = ""
    private var lastAbsolutePath: String = "" // Guardamos la ruta para devolverla en stop()

    companion object {
        // Parámetros de grabación de audio:
        // 44100 Hz = calidad de CD (estándar para grabaciones de voz)
        private const val SAMPLE_RATE = 44100

        // CHANNEL_IN_MONO = un solo canal de audio (mono)
        // Para voz es suficiente; estéreo (2 canales) solo se necesita en música
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO

        // PCM_16BIT = 16 bits por muestra (rango -32768 a 32767)
        // Es el formato más compatible y suficiente para grabación de voz
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    /**
     * Inicia la grabación de audio y escribe los datos al archivo especificado.
     *
     * Pasos internos:
     * 1. Detiene cualquier grabación anterior (por seguridad).
     * 2. Calcula el tamaño mínimo del buffer de AudioRecord.
     * 3. Inicializa AudioRecord y lo pone en estado de grabación.
     * 4. Lanza una coroutine que lee PCM del micrófono en un loop.
     *
     * @param path Nombre del archivo (ej. "nota_voz_123.wav") o ruta absoluta.
     * @throws SecurityException si no se tiene el permiso RECORD_AUDIO.
     * @throws IllegalStateException si AudioRecord no se pudo inicializar.
     */
    actual fun startRecording(path: String) {
        stopRecording()  // Asegurar que no hay grabación previa activa

        val filesDir = androidContext?.filesDir
        if (filesDir == null) {
            AppLogger.e("AudioRecorder", "Error: contexto de aplicación no disponible")
            throw IllegalStateException("Application context not available")
        }

        currentRecordedPath = path
        // Si la ruta no es absoluta, la construimos relativa a filesDir
        val absolutePath = if (path.startsWith("/")) path else File(filesDir, path).absolutePath
        lastAbsolutePath = absolutePath

        File(absolutePath).parentFile?.mkdirs()  // Crear directorios intermedios si faltan
        File(absolutePath).delete()              // Borrar archivo anterior del mismo nombre

        AppLogger.d("AudioRecorder", "Iniciando grabación con AudioRecord - path: $absolutePath")

        try {
            // getMinBufferSize calcula el tamaño mínimo del buffer para estos parámetros.
            // Un buffer muy pequeño puede causar pérdida de datos (overrun).
            // Por eso usamos bufferSize * 2 al crear AudioRecord.
            val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            AppLogger.d("AudioRecorder", "Buffer size mínimo: $bufferSize")

            // MediaRecorder.AudioSource.MIC = fuente de audio: el micrófono del dispositivo
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize * 2  // Buffer doble para mayor estabilidad
            )

            // Verificar que AudioRecord se inicializó correctamente
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                AppLogger.e("AudioRecorder", "AudioRecord no se inicializó. Estado: ${audioRecord?.state}")
                throw IllegalStateException("AudioRecord no inicializado")
            }

            isRecording = true
            audioRecord?.startRecording()  // Empieza a capturar audio del micrófono
            AppLogger.d("AudioRecorder", "✅ AudioRecord iniciado - state: ${audioRecord?.state}")

            // Lanzamos una coroutine en el dispatcher IO para la escritura al disco.
            // El dispatcher IO tiene un pool de hilos diseñado para operaciones de I/O,
            // por lo que no bloquea el hilo principal de la UI.
            val scope = CoroutineScope(Dispatchers.IO)
            recordingJob = scope.launch {
                writeAudioDataToFile(absolutePath, bufferSize)
            }

        } catch (e: SecurityException) {
            AppLogger.e("AudioRecorder", "❌ Error de permisos: ${e.message}")
            throw e  // Re-lanzamos para que App.kt maneje el caso (solicitar permiso)
        } catch (e: Exception) {
            AppLogger.e("AudioRecorder", "❌ Error al iniciar grabación: ${e.message}")
            AppLogger.e("AudioRecorder", "Stack: ${e.stackTraceToString()}")
            cleanup()
            throw e
        }
    }

    /**
     * Loop de escritura: lee PCM del micrófono y lo escribe al archivo.
     *
     * Esta función corre en una coroutine (hilo IO) mientras `isRecording` sea true.
     * Es una función `suspend` porque usa `currentCoroutineContext().isActive`
     * para respetar la cancelación de la coroutine.
     *
     * Estructura del archivo de salida:
     *   [44 bytes de header WAV vacío]  ← se actualiza al terminar con updateWavHeader()
     *   [bytes PCM del micrófono...]    ← se agregan en cada iteración del loop
     *
     * @param filePath  Ruta absoluta del archivo de destino.
     * @param bufferSize Tamaño del buffer de lectura en bytes.
     */
    private suspend fun writeAudioDataToFile(filePath: String, bufferSize: Int) {
        val buffer = ByteArray(bufferSize)
        val outputStream = FileOutputStream(filePath)

        // Reservar espacio para el header WAV (44 bytes de ceros).
        // Se llenará con los valores correctos al terminar la grabación.
        val wavHeader = ByteArray(44)
        outputStream.write(wavHeader)

        var totalBytesWritten = 0L

        try {
            // `currentCoroutineContext().isActive` es el mecanismo de cancelación
            // de las coroutines. Cuando se cancela la Job, este flag pasa a false
            // y el loop termina limpiamente.
            while (currentCoroutineContext().isActive && isRecording) {
                val read = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                if (read > 0) {
                    outputStream.write(buffer, 0, read)
                    totalBytesWritten += read
                }
            }
        } finally {
            // `finally` se ejecuta SIEMPRE, incluso si hay cancelación o excepción
            outputStream.close()
            AppLogger.d("AudioRecorder", "Grabación terminada. Total bytes: $totalBytesWritten")

            // Ahora que sabemos el tamaño real, completamos el header WAV
            if (totalBytesWritten > 0) {
                updateWavHeader(filePath, totalBytesWritten)
            }
        }
    }

    /**
     * Escribe el header WAV estándar al inicio del archivo.
     *
     * El formato WAV (RIFF) tiene una estructura de 44 bytes:
     *
     * Offset  Bytes  Contenido
     * ------  -----  ---------
     *  0       4     "RIFF"           ← identificador del formato
     *  4       4     tamaño total - 8 ← tamaño del archivo menos los primeros 8 bytes
     *  8       4     "WAVE"           ← sub-formato
     * 12       4     "fmt "           ← inicio del bloque de formato
     * 16       4     16               ← tamaño del bloque fmt (siempre 16 para PCM)
     * 20       2     1                ← AudioFormat (1 = PCM sin compresión)
     * 22       2     1                ← NumChannels (1 = mono)
     * 24       4     44100            ← SampleRate
     * 28       4     88200            ← ByteRate = SampleRate × NumChannels × BitsPerSample/8
     * 32       2     2                ← BlockAlign = NumChannels × BitsPerSample/8
     * 34       2     16               ← BitsPerSample
     * 36       4     "data"           ← inicio de los datos de audio
     * 40       4     tamaño datos     ← tamaño en bytes de los datos PCM
     * 44+      ∞     datos PCM        ← los bytes de audio grabado
     *
     * @param filePath  Ruta del archivo a modificar.
     * @param dataSize  Cantidad de bytes PCM grabados (sin el header).
     */
    private fun updateWavHeader(filePath: String, dataSize: Long) {
        try {
            // RandomAccessFile permite leer Y escribir en posiciones arbitrarias del archivo.
            // El modo "rw" = read-write. Es la única forma de modificar bytes al principio
            // de un archivo que ya tiene datos al final.
            val raf = RandomAccessFile(filePath, "rw")

            // Los números en WAV se almacenan en little-endian (byte menos significativo primero).
            // Por eso usamos intToByteArray() y shortToByteArray() que hacen esa conversión.

            raf.seek(0)  // Mover el cursor al inicio del archivo
            raf.writeBytes("RIFF")
            raf.write(intToByteArray((36 + dataSize).toInt()))  // 36 = tamaño del header sin RIFF/size
            raf.writeBytes("WAVE")

            raf.writeBytes("fmt ")
            raf.write(intToByteArray(16))            // Subchunk1Size: 16 para PCM
            raf.write(shortToByteArray(1))           // AudioFormat: 1 = PCM
            raf.write(shortToByteArray(1))           // NumChannels: 1 = mono
            raf.write(intToByteArray(SAMPLE_RATE))   // SampleRate: 44100 Hz
            raf.write(intToByteArray(SAMPLE_RATE * 1 * 16 / 8))  // ByteRate
            raf.write(shortToByteArray(1 * 16 / 8))  // BlockAlign
            raf.write(shortToByteArray(16))           // BitsPerSample: 16

            raf.writeBytes("data")
            raf.write(intToByteArray(dataSize.toInt()))  // Tamaño de los datos PCM

            raf.close()
            AppLogger.d("AudioRecorder", "Header WAV actualizado")
        } catch (e: Exception) {
            AppLogger.e("AudioRecorder", "Error actualizando header WAV: ${e.message}")
        }
    }

    /**
     * Convierte un Int a 4 bytes en formato little-endian.
     *
     * Little-endian = el byte menos significativo va primero.
     * WAV y la mayoría de formatos de PC usan little-endian.
     *
     * Ejemplo: 44100 (0x0000AC44) → [0x44, 0xAC, 0x00, 0x00]
     *
     * Las operaciones:
     * - `and 0xFF`    → toma solo el byte menos significativo
     * - `shr 8`       → desplaza 8 bits a la derecha (siguiente byte)
     * - `.toByte()`   → convierte a Byte (que puede ser negativo en Kotlin/JVM)
     */
    private fun intToByteArray(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte()
        )
    }

    /** Convierte un Int a 2 bytes en formato little-endian (para campos de 16 bits en WAV). */
    private fun shortToByteArray(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte()
        )
    }

    /**
     * Detiene la grabación activa y devuelve la ruta del archivo grabado.
     *
     * @return Ruta absoluta del archivo WAV grabado.
     */
    actual fun stopRecording(): String {
        AppLogger.d("AudioRecorder", "Deteniendo grabación...")

        isRecording = false         // Señal para que el loop en writeAudioDataToFile termine
        recordingJob?.cancel()      // Cancela la coroutine si aún está activa
        recordingJob = null

        try {
            audioRecord?.stop()     // Deja de capturar del micrófono
            audioRecord?.release()  // Libera el recurso de hardware del micrófono
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

    /**
     * Limpia todos los recursos de grabación en caso de error.
     * Se llama desde el bloque catch de startRecording().
     */
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

/**
 * Crea y recuerda un [AudioRecorder] ligado al ciclo de vida del Composable.
 */
@Composable
actual fun rememberAudioRecorder(): AudioRecorder {
    val context = LocalContext.current
    return remember { AudioRecorder(context) }
}

// TODO: El CoroutineScope creado en startRecording() no está ligado a ningún
//       ciclo de vida. Si el usuario sale de la app sin detener la grabación,
//       la coroutine continúa activa. Debería recibirse un scope externo del
//       ViewModel o usarse viewModelScope para garantizar cancelación automática.
//
// TODO: Agregar un método `cancelRecording()` que elimine el archivo temporal,
//       para cuando el usuario cancela (ej. desliza para cancelar).
//
// TODO: El header WAV puede fallar si `dataSize` supera Int.MAX_VALUE (~2GB).
//       Aunque improbable para notas de voz, es un bug latente. Usar chunks
//       RIFF64 o limitar la duración de grabación (ej. máximo 5 minutos).
//
// TODO: Considerar comprimir el audio a AAC/M4A con MediaRecorder para reducir
//       el tamaño del archivo. Un WAV de 1 minuto a 44100Hz mono ocupa ~5 MB,
//       mientras que el mismo audio en AAC ocupa ~600 KB.
