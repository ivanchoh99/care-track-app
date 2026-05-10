package com.app.caretrack.common

import Foundation

// =============================================================================
// IMPLEMENTACIÓN iOS DE AppLogger (actual)
// =============================================================================
// En iOS, el equivalente de `android.util.Log` es `NSLog` de Foundation.
// NSLog escribe mensajes en la consola de Xcode y en el sistema de logs de iOS.
//
// Para ver los logs en iOS:
//   - Xcode → Debug area (cmd+shift+Y) mientras la app corre en simulador/device
//   - Console.app de macOS → filtra por el nombre de la app
//
// Nota: En producción (App Store), NSLog también escribe en el log del sistema,
// lo que podría exponer información sensible. Considerar desactivarlo en release.
// =============================================================================

/**
 * Implementación iOS de [AppLogger] usando `NSLog` de Foundation.
 *
 * El formato del mensaje incluye el nivel y el tag para facilitar el filtrado:
 * - "ERROR/ChatRepository: mensaje"
 * - "WARN/AudioPlayer: mensaje"
 * - "DEBUG/WebSocket: mensaje"
 *
 * `Foundation` es el framework base de Apple, equivalente a java.lang + java.util.
 * Se importa automáticamente en el código Kotlin de iosMain.
 */
actual object AppLogger {
    actual fun e(tag: String, message: String) { NSLog("ERROR/%@: %@", tag, message) }
    actual fun w(tag: String, message: String) { NSLog("WARN/%@: %@", tag, message) }
    actual fun d(tag: String, message: String) { NSLog("DEBUG/%@: %@", tag, message) }
}

// TODO: Considerar usar `os_log` de Apple en lugar de NSLog. `os_log` es más
//       eficiente (buffered, lazy evaluation) y se integra mejor con Instruments
//       para profiling. NSLog es más simple pero escribe síncronamente.
//
// TODO: Agregar niveles de log condicionados al entorno (debug vs release)
//       usando `#if DEBUG` o una flag de compilación de Kotlin/Native.
