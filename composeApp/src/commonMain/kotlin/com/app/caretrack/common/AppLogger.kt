package com.app.caretrack.common

// =============================================================================
// PATRÓN EXPECT/ACTUAL — EL CORAZÓN DE KOTLIN MULTIPLATFORM
// =============================================================================
// En KMP, el código en `commonMain` se compila para TODAS las plataformas.
// Pero a veces necesitas usar APIs específicas de cada plataforma
// (ej. `android.util.Log` en Android, `NSLog` en iOS).
//
// La solución es el par `expect` / `actual`:
//
//   EXPECT (en commonMain) → "Prometo que existirá esto en cada plataforma"
//   ACTUAL (en androidMain / iosMain) → "Aquí está la implementación real"
//
// El compilador verifica que cada plataforma tenga su `actual` correspondiente.
// Si falta alguno, el proyecto no compila. Es como una interfaz, pero a nivel
// de plataforma.
//
// Este archivo define el "contrato" (expect).
// Las implementaciones están en:
//   - AppLogger.android.kt  → usa android.util.Log
//   - AppLogger.ios.kt      → usa NSLog de Foundation
// =============================================================================

/**
 * Logger multiplataforma para registrar mensajes de depuración y errores.
 *
 * `expect object` declara un singleton que DEBE tener implementación en cada
 * plataforma. En Android se mapea a `Log.d/w/e`, en iOS a `NSLog`.
 *
 * Úsalo en lugar de `println()` porque:
 * 1. En Android, los logs aparecen en Logcat con nivel de severidad correcto.
 * 2. En iOS, aparecen en la consola de Xcode.
 * 3. Permite filtrar por `tag` para encontrar logs de un componente específico.
 *
 * Niveles de log (de menor a mayor severidad):
 * - `d` (debug)   → Información para desarrollo. No aparece en builds release.
 * - `w` (warning) → Algo inesperado pero la app puede continuar.
 * - `e` (error)   → Algo salió mal. Úsalo cuando hay un fallo real.
 *
 * Ejemplo de uso:
 * ```kotlin
 * AppLogger.d("ChatRepository", "Mensaje enviado correctamente")
 * AppLogger.e("AudioPlayer", "No se pudo abrir el archivo: $path")
 * ```
 */
expect object AppLogger {
    fun e(tag: String, message: String)
    fun w(tag: String, message: String)
    fun d(tag: String, message: String)
}

// TODO: En builds de producción (release), los logs de nivel `d` deberían
//       desactivarse para no exponer información sensible. Considerar agregar
//       una constante `BuildConfig.DEBUG` y verificarla antes de loguear.
//
// TODO: Para un sistema de logging más robusto a futuro, evaluar una librería
//       multiplataforma como Kermit (https://github.com/touchlab/Kermit)
//       que soporta múltiples destinos (consola, archivo, Crashlytics, etc.)
