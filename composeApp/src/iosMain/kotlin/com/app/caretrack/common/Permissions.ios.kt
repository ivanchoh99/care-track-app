package com.app.caretrack.common

import UIKit
import AVFoundation
import Photos
import MobileCoreServices

// =============================================================================
// IMPLEMENTACIÓN iOS DE PERMISOS (actual) — STUB / INCOMPLETO
// =============================================================================
// ⚠️ ADVERTENCIA: Esta implementación contiene código que NO compila en iOS
// porque mezcla sintaxis de Kotlin y Swift/Objective-C.
//
// En iOS, los permisos de micrófono se solicitan con AVFoundation:
//   AVAudioSession.sharedInstance().requestRecordPermission { granted in ... }
//
// La implementación correcta para Kotlin/Native (KMP en iOS) debería usar
// las APIs de AVFoundation expuestas por el interop de Kotlin con Objective-C.
//
// Estado actual: stub que siempre devuelve `true` (permiso otorgado).
// Esto permite que el código compile pero no solicita el permiso real en iOS.
// =============================================================================

/**
 * Implementación iOS de [rememberPermissionLauncher].
 *
 * ⚠️ IMPLEMENTACIÓN PENDIENTE: Actualmente devuelve un launcher que siempre
 * reporta el permiso como otorgado sin verificar el estado real de iOS.
 *
 * La implementación correcta debería llamar a:
 * ```kotlin
 * AVAudioSession.sharedInstance().requestRecordPermission { granted ->
 *     onResult(granted)
 * }
 * ```
 *
 * @param onResult Callback con el resultado del permiso (actualmente siempre true).
 */
@Composable
actual fun rememberPermissionLauncher(onResult: (Boolean) -> Unit): PermissionRequestLauncher {
    return object : PermissionRequestLauncher {
        override fun launch() {
            // TODO: Implementar solicitud real de permiso de micrófono en iOS.
            //       Actualmente solo notifica que el permiso fue "otorgado" sin
            //       verificarlo, lo que significa que la grabación fallará en
            //       dispositivos iOS reales que no tienen el permiso.
            onResult(true)
        }
    }
}

/**
 * Implementación iOS de [checkInitialAudioPermission].
 *
 * ⚠️ IMPLEMENTACIÓN PENDIENTE: Actualmente siempre devuelve `true`.
 *
 * La implementación correcta debería verificar el estado actual del permiso:
 * ```kotlin
 * return AVAudioSession.sharedInstance().recordPermission == AVAudioSessionRecordPermission.granted
 * ```
 */
actual fun checkInitialAudioPermission(): Boolean {
    // TODO: Verificar el estado real del permiso de micrófono en iOS usando
    //       AVAudioSession.sharedInstance().recordPermission.
    //       Valores posibles: .undetermined, .denied, .granted
    return true
}

// TODO: Esta es la implementación más crítica pendiente para el soporte iOS.
//       Sin permisos reales, la grabación de audio no funcionará en iOS.
//       La Info.plist del target iOS también necesita la clave:
//       NSMicrophoneUsageDescription = "CareTrack necesita el micrófono para notas de voz"
