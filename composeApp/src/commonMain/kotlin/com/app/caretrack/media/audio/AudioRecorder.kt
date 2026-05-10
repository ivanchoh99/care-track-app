package com.app.caretrack.media.audio

import androidx.compose.runtime.Composable

// =============================================================================
// GRABADOR DE AUDIO — INTERFAZ MULTIPLATAFORMA (expect)
// =============================================================================
// La grabación de audio requiere APIs nativas muy diferentes:
//   Android → AudioRecord (PCM raw) o MediaRecorder (formatos comprimidos)
//   iOS     → AVAudioRecorder de AVFoundation
//
// La implementación Android actual usa `AudioRecord` directamente para tener
// control total sobre el PCM y escribir el header WAV manualmente.
// Esto da mayor flexibilidad pero también más complejidad.
//
// Archivos de implementación:
//   - AudioRecorder.android.kt → implementación con AudioRecord + WAV encoding
//   - AudioRecorder.ios.kt     → stub pendiente de implementación real
// =============================================================================

/**
 * Grabador de audio multiplataforma.
 *
 * El valor por defecto `context: Any? = null` permite crear instancias sin
 * pasar contexto (útil para iOS donde no se necesita Context de Android).
 *
 * @param context En Android: el `android.content.Context` de la Activity.
 *                En iOS: se pasa null (no necesario con AVFoundation).
 */
expect class AudioRecorder(context: Any? = null) {

    /**
     * Inicia la grabación de audio y la guarda en el archivo especificado.
     *
     * Si ya había una grabación en curso, la detiene primero.
     * Requiere que el permiso `RECORD_AUDIO` haya sido otorgado ANTES de llamar.
     *
     * @param path Nombre del archivo (relativo al directorio `filesDir`)
     *             o ruta absoluta. Ej: `"nota_voz_1234567890.m4a"`.
     * @throws SecurityException si el permiso de audio no fue otorgado.
     * @throws IllegalStateException si el contexto no está disponible.
     */
    fun startRecording(path: String)

    /**
     * Detiene la grabación activa y finaliza la escritura del archivo.
     *
     * En la implementación Android, este método también completa el
     * header WAV con el tamaño real de los datos grabados.
     *
     * @return La ruta absoluta del archivo guardado, o cadena vacía si hubo error.
     */
    fun stopRecording(): String
}

/**
 * Crea y recuerda una instancia de [AudioRecorder] ligada al ciclo de vida
 * del Composable. La instancia se mantiene entre recomposiciones.
 *
 * @see rememberAudioPlayer para la misma idea aplicada a la reproducción.
 */
@Composable
expect fun rememberAudioRecorder(): AudioRecorder

// TODO: El `AudioRecorder` actual crea su propio `CoroutineScope(Dispatchers.IO)`
//       internamente, lo que puede causar un memory leak si el recorder no se
//       limpia correctamente. Debería recibir un scope externo (del ViewModel
//       o del Composable) para garantizar la cancelación automática.
//
// TODO: Agregar un método `cancelRecording()` que detenga la grabación y
//       elimine el archivo temporal, para el caso en que el usuario cancela
//       una nota de voz (actualmente no existe esta funcionalidad).
//
// TODO: La implementación iOS (AudioRecorder.ios.kt) usa MediaRecorder de Android.
//       Debe implementarse con AVAudioRecorder para que funcione en iOS.
//
// TODO: Considerar grabar en formato M4A/AAC directamente (más compacto que WAV)
//       usando MediaRecorder en Android en lugar de AudioRecord + WAV manual,
//       a menos que se necesite el control a bajo nivel del PCM.
