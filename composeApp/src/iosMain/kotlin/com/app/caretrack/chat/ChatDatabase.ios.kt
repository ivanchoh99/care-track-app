package com.app.caretrack.chat

// =============================================================================
// IMPLEMENTACIÓN iOS DEL BUILDER DE ROOM — STUB
// =============================================================================
// Room es una librería de Jetpack (Google) diseñada para Android/JVM.
// En iOS (Kotlin/Native), Room no está disponible de la misma forma.
//
// Para persistencia de datos en KMP multiplataforma, las alternativas son:
//   1. SQLDelight → librería multiplataforma que genera código SQL tipado
//      y funciona tanto en Android como en iOS nativo.
//   2. Room + BundledSQLiteDriver → funciona en Android. El soporte para
//      iOS está en desarrollo pero aún experimental.
//
// Por ahora, la implementación iOS lanza NotImplementedError, lo que significa
// que la app crasheará en iOS si se intenta usar la base de datos.
// =============================================================================

/**
 * ⚠️ NO IMPLEMENTADO — Implementación iOS de [instantiateDatabaseBuilder].
 *
 * Room no está disponible de la misma forma en iOS/Kotlin-Native.
 * Esta función lanza NotImplementedError para que el error sea explícito
 * (mejor que una excepción críptica o comportamiento indefinido).
 *
 * @throws NotImplementedError siempre — la base de datos no funciona en iOS.
 */
actual fun instantiateDatabaseBuilder(context: Any?): androidx.room.RoomDatabase.Builder<ChatDatabase> {
    // TODO: Migrar a SQLDelight para tener soporte real de base de datos en iOS.
    //       SQLDelight genera Kotlin nativo para ambas plataformas y es el estándar
    //       de facto para persistencia en proyectos KMP.
    //       Guía de migración: https://cashapp.github.io/sqldelight/
    throw NotImplementedError("Room database is not available on iOS in the same way")
}

// TODO: CRÍTICO para soporte iOS — Implementar persistencia local en iOS.
//       Opciones:
//       1. SQLDelight (recomendado): multiplataforma, buena integración con KMP
//       2. Realm Kotlin: ORM multiplataforma con buen soporte KMP
//       3. Room con BundledSQLiteDriver: experimental, puede funcionar con iOS
//          si se configura correctamente en el Gradle del módulo iOS.
//
// TODO: Si se elige SQLDelight, habrá que migrar:
//       - ChatDao (queries SQL) → archivos .sq de SQLDelight
//       - MessageEntity → tipos de SQLDelight (no @Entity de Room)
//       - ChatDatabase → DatabaseDriverFactory de SQLDelight
