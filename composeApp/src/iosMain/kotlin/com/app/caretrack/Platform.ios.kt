package com.app.caretrack

import platform.UIKit.UIDevice

// =============================================================================
// INFORMACIÓN DE PLATAFORMA — iOS
// =============================================================================
// Este archivo provee información del dispositivo iOS en tiempo de ejecución.
// Es parte del sistema expect/actual: `getPlatform()` está declarada como
// `expect fun` en algún lugar de commonMain y esta es su implementación iOS.
//
// `platform.UIKit.UIDevice` es la clase de UIKit (framework de iOS) que
// provee información sobre el dispositivo: modelo, versión de iOS, etc.
//
// Ejemplo de valor devuelto: "iOS 17.2"
// =============================================================================

/**
 * Implementación iOS de la interfaz [Platform].
 *
 * `UIDevice.currentDevice` es el singleton que representa el dispositivo actual.
 * - `systemName()` devuelve "iOS" (o "iPadOS" en tablets).
 * - `systemVersion` devuelve la versión del SO, ej. "17.2.1".
 */
class IOSPlatform : Platform {
    override val name: String =
        UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

/**
 * Implementación iOS de [getPlatform()].
 *
 * Devuelve una instancia de [IOSPlatform] con la información del dispositivo iOS actual.
 */
actual fun getPlatform(): Platform = IOSPlatform()

// TODO: La interfaz `Platform` y la función `expect fun getPlatform()` deberían
//       estar definidas en commonMain, pero no se encontraron en los archivos del proyecto.
//       Verificar si existe el archivo Platform.kt en commonMain o si fue eliminado.
//       Si no se usa `getPlatform()` en ningún lugar de la app, considerar eliminar
//       este archivo y la interfaz Platform para limpiar código muerto.
