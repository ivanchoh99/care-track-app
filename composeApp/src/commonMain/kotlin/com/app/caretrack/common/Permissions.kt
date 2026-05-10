package com.app.caretrack.common

import androidx.compose.runtime.Composable

// =============================================================================
// PERMISOS EN TIEMPO DE EJECUCIÓN — ABSTRACCIÓN MULTIPLATAFORMA
// =============================================================================
// Android e iOS manejan permisos de forma muy diferente:
//
// Android → El usuario acepta un dialog del sistema. Se usa
//           `ActivityResultContracts.RequestPermission()` con un launcher.
//
// iOS     → El sistema muestra el dialog automáticamente la primera vez que
//           intentas usar el micrófono. El permiso puede chequearse con
//           `AVAudioSession.sharedInstance().recordPermission`.
//
// Con expect/actual abstraemos estas diferencias y el código común
// (App.kt) solo llama a `rememberPermissionLauncher` sin saber cómo
// funciona en cada plataforma.
// =============================================================================

/**
 * Interfaz que representa el objeto capaz de lanzar un request de permiso.
 *
 * Es una interfaz normal de Kotlin (no expect/actual) porque la definición
 * es la misma en todas las plataformas. Solo la implementación difiere,
 * y eso se maneja dentro de las funciones `actual` de abajo.
 *
 * Uso:
 * ```kotlin
 * val launcher = rememberPermissionLauncher { isGranted ->
 *     if (isGranted) iniciarGrabacion()
 * }
 * // Más tarde, cuando el usuario presiona el botón:
 * launcher.launch()
 * ```
 */
interface PermissionRequestLauncher {
    /** Muestra el diálogo de solicitud de permiso al usuario. */
    fun launch()
}

/**
 * Crea y recuerda un launcher de permisos de audio.
 *
 * `@Composable` → Esta función solo puede llamarse desde dentro de otros
 * Composables. El prefijo `remember` en el nombre es una convención de Compose:
 * indica que el objeto se recuerda entre recomposiciones.
 *
 * `expect` → Cada plataforma provee su implementación:
 * - Android: usa `rememberLauncherForActivityResult`
 * - iOS:     usa AVFoundation para solicitar permiso de micrófono
 *
 * @param onResult Callback que se ejecuta cuando el usuario responde al diálogo.
 *                 Recibe `true` si el permiso fue otorgado, `false` si fue negado.
 */
@Composable
expect fun rememberPermissionLauncher(onResult: (Boolean) -> Unit): PermissionRequestLauncher

/**
 * Verifica si el permiso de audio YA fue otorgado (sin mostrar ningún diálogo).
 *
 * Llamar esto al iniciar la app permite saber si ya tenemos el permiso de sesiones
 * anteriores, evitando solicitar permisos innecesariamente.
 *
 * `expect` → La verificación se hace diferente en cada plataforma:
 * - Android: consulta `ContextCompat.checkSelfPermission`
 * - iOS:     consulta `AVAudioSession.recordPermission`
 *
 * @return `true` si el permiso de RECORD_AUDIO ya fue otorgado.
 */
expect fun checkInitialAudioPermission(): Boolean

// TODO: La implementación Android de checkInitialAudioPermission() actualmente
//       siempre devuelve `false` (hardcodeado). Debe implementarse correctamente
//       para consultar el estado real del permiso desde el contexto de la app.
//       Sin esto, la app siempre pide el permiso incluso si ya fue otorgado.
//
// TODO: Generalizar esta abstracción para soportar más permisos en el futuro
//       (cámara, notificaciones, etc.) cuando se implementen más funcionalidades.
//       Considerar un enum `AppPermission` como parámetro en lugar de hardcodear
//       RECORD_AUDIO dentro de la implementación.
