package com.app.caretrack.media.audio

import androidx.compose.runtime.Composable

// =============================================================================
// REPRODUCTOR DE AUDIO — INTERFAZ MULTIPLATAFORMA (expect)
// =============================================================================
// El reproductor de audio es muy diferente en cada plataforma:
//   Android → MediaPlayer de android.media
//   iOS     → AVAudioPlayer de AVFoundation
//
// Con `expect class` definimos el contrato: "toda plataforma tendrá una clase
// AudioPlayer con estos métodos". Cada plataforma provee el `actual`.
//
// Archivos de implementación:
//   - AudioPlayer.android.kt → implementación con MediaPlayer
//   - AudioPlayer.ios.kt     → implementación con AVAudioPlayer (pendiente)
// =============================================================================

/**
 * Reproductor de audio multiplataforma.
 *
 * `expect class` funciona como una clase abstracta: declara la estructura
 * pero delega la implementación a cada plataforma.
 *
 * El parámetro `context: Any?` es un truco de KMP: en Android necesitamos
 * el `Context` de Android para muchas operaciones, pero en commonMain no
 * existe esa clase. Usamos `Any?` como tipo genérico y hacemos el cast
 * dentro de cada implementación `actual`.
 *
 * @param context En Android: el `android.content.Context` de la Activity.
 *                En iOS: no se usa (puede ser null).
 */
expect class AudioPlayer(context: Any?) {

    /**
     * Reproduce un archivo de audio desde una ruta local o URL.
     *
     * @param path Ruta absoluta del archivo en el dispositivo
     *             (ej. `/data/user/0/com.app.caretrack/files/nota.wav`)
     *             o URL remota (ej. `https://servidor.com/audio/123.mp3`).
     */
    fun playAudio(path: String)

    /**
     * Reproduce audio directamente desde bytes en memoria.
     * Útil cuando el audio proviene de la red y no está guardado localmente.
     *
     * @param bytes El contenido del archivo de audio en memoria.
     */
    fun playAudioFromBytes(bytes: ByteArray)

    /** Detiene la reproducción actual y libera los recursos del reproductor. */
    fun stopAudio()

    /**
     * Devuelve la duración total del audio cargado en milisegundos.
     * Devuelve 0 si no hay audio cargado o si aún no terminó de prepararse.
     */
    fun getDuration(): Int

    /**
     * Devuelve la posición actual de reproducción en milisegundos.
     * Se usa para actualizar la barra de progreso en la UI.
     */
    fun getCurrentPosition(): Int

    /** Devuelve `true` si hay reproducción activa en este momento. */
    fun isPlaying(): Boolean
}

/**
 * Crea y recuerda una instancia de [AudioPlayer] en el contexto de Compose.
 *
 * El prefijo `remember` indica que la instancia sobrevive a las recomposiciones
 * de la pantalla. Sin `remember`, se crearía un nuevo AudioPlayer en cada
 * redibujado, perdiendo el estado de reproducción.
 *
 * `@Composable` → Solo puede llamarse desde dentro de Composables.
 * `expect` → Cada plataforma define cómo crear y recordar su AudioPlayer.
 */
@Composable
expect fun rememberAudioPlayer(): AudioPlayer

// TODO: Agregar un método `pause()` separado de `stopAudio()`. Actualmente
//       "pausa" y "detiene" son la misma operación, lo que hace que al
//       presionar play de nuevo el audio empiece desde el principio en
//       lugar de continuar desde donde estaba.
//
// TODO: Agregar un método `seekTo(positionMs: Int)` para permitir al usuario
//       adelantar o retroceder en el audio arrastrando la barra de progreso.
//
// TODO: Agregar callback `onCompletion: () -> Unit` para que la UI sepa
//       automáticamente cuándo terminó la reproducción, en lugar del polling
//       cada 100ms que se hace en AudioMessageBubble.
//
// TODO: La implementación iOS (AudioPlayer.ios.kt) actualmente usa MediaPlayer
//       de Android, lo que es un error. Debe implementarse con AVAudioPlayer
//       de iOS para que la app funcione correctamente en iPhone/iPad.
