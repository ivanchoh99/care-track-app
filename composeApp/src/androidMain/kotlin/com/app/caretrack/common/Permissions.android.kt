package com.app.caretrack.common

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

// =============================================================================
// IMPLEMENTACIÓN ANDROID DE PERMISOS (actual)
// =============================================================================
// Android requiere que los permisos "peligrosos" (como acceder al micrófono)
// sean solicitados explícitamente al usuario en tiempo de ejecución (desde API 23).
//
// El flujo en Android es:
//   1. App llama `launcher.launch(permiso)`
//   2. Android muestra un dialog del sistema al usuario
//   3. El usuario acepta o rechaza
//   4. Android llama nuestro callback `onResult(isGranted)`
//
// `rememberLauncherForActivityResult` es la forma moderna (Jetpack) de
// manejar esto sin tener que sobreescribir `onRequestPermissionsResult`.
// =============================================================================

/**
 * Implementación Android de [rememberPermissionLauncher].
 *
 * Crea un launcher para solicitar el permiso `RECORD_AUDIO`.
 *
 * `ActivityResultContracts.RequestPermission()` es el "contrato" moderno de
 * Android para pedir un único permiso. El resultado es un Boolean (granted/denied).
 *
 * Devuelve un objeto anónimo que implementa [PermissionRequestLauncher].
 * Un "objeto anónimo" en Kotlin es como una clase sin nombre que implementa
 * una interfaz al vuelo — equivalente a una clase anónima en Java.
 *
 * @param onResult Callback: `true` si el usuario otorgó el permiso.
 */
@Composable
actual fun rememberPermissionLauncher(onResult: (Boolean) -> Unit): PermissionRequestLauncher {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onResult(isGranted)
    }

    // Retornamos una implementación de la interfaz que llama al launcher de Android
    return object : PermissionRequestLauncher {
        override fun launch() {
            // Manifest.permission.RECORD_AUDIO es la constante de la string
            // "android.permission.RECORD_AUDIO" del AndroidManifest.xml
            launcher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}

/**
 * Implementación Android de [checkInitialAudioPermission].
 *
 * ⚠️ ATENCIÓN: Esta implementación está incorrecta — siempre devuelve `false`.
 * Debería verificar el estado real del permiso con ContextCompat.
 *
 * La implementación correcta sería:
 * ```kotlin
 * val context = ... // necesitamos el contexto aquí
 * return ContextCompat.checkSelfPermission(
 *     context,
 *     Manifest.permission.RECORD_AUDIO
 * ) == PackageManager.PERMISSION_GRANTED
 * ```
 *
 * El problema es que esta función no es `@Composable`, por lo que no puede
 * acceder a `LocalContext.current`. Ver TODO abajo.
 */
actual fun checkInitialAudioPermission(): Boolean {
    // TODO: Esta función siempre devuelve false, lo que causa que la app siempre
    //       muestre el diálogo de permisos aunque ya fueron otorgados.
    //       Soluciones posibles:
    //       1. Convertirla en @Composable y usar LocalContext.current
    //       2. Pasarle el Context como parámetro (requiere cambiar la firma en
    //          commonMain también, usando Any? como tipo)
    //       3. Usar un ContextHolder singleton (patrón común en Android)
    return false
}
