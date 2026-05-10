package com.app.caretrack.common

import android.util.Log

// =============================================================================
// IMPLEMENTACIÓN ANDROID DE AppLogger (actual)
// =============================================================================
// Este archivo implementa el contrato definido en AppLogger.kt (commonMain).
// La palabra clave `actual` le dice al compilador: "aquí está la implementación
// real para Android de lo que prometí con `expect`".
//
// `android.util.Log` es el sistema de logging nativo de Android.
// Los logs aparecen en Logcat (Android Studio → View → Tool Windows → Logcat).
//
// Para ver los logs de esta app en Logcat, filtra por el tag que uses, ej:
//   tag:ChatRepository  → muestra solo logs de ese componente
//   level:ERROR         → muestra solo errores
// =============================================================================

/**
 * Implementación Android de [AppLogger] usando `android.util.Log`.
 *
 * `actual object` significa que este es el singleton real para Android.
 * El compilador KMP lo une con el `expect object AppLogger` de commonMain.
 */
actual object AppLogger {
    /** Log de error — para fallos reales que el usuario puede notar. */
    actual fun e(tag: String, message: String) { Log.e(tag, message) }

    /** Log de advertencia — algo inesperado pero recuperable. */
    actual fun w(tag: String, message: String) { Log.w(tag, message) }

    /** Log de debug — información para desarrollo. Solo visible en Logcat. */
    actual fun d(tag: String, message: String) { Log.d(tag, message) }
}

// TODO: Agregar soporte para loguear excepciones. La versión actual solo loguea
//       el mensaje, pero `Log.e(tag, message, throwable)` también muestra
//       el stack trace completo, que es invaluable para depurar crashes.
//       Ejemplo: `actual fun e(tag: String, message: String, throwable: Throwable? = null)`
//
// TODO: En builds de producción (BuildConfig.DEBUG == false), omitir los logs
//       de nivel `d` para no exponer lógica interna de la app a usuarios finales.
