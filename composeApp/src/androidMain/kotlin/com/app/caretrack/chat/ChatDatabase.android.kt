package com.app.caretrack.chat

import android.content.Context
import androidx.room.Room

// =============================================================================
// IMPLEMENTACIÓN ANDROID DEL BUILDER DE ROOM (actual)
// =============================================================================
// Room necesita un "builder" para crear la base de datos con la configuración
// correcta. En Android, esto requiere el Context (para saber dónde guardar
// el archivo SQLite). En commonMain no existe Context, así que usamos
// el patrón expect/actual para abstraer esta diferencia.
//
// El archivo de la base de datos se guarda en:
//   /data/data/com.app.caretrack/databases/caretrack_chat_database
//
// Este directorio es privado para la app y no requiere permisos especiales.
// =============================================================================

/**
 * Implementación Android de [instantiateDatabaseBuilder].
 *
 * `Room.databaseBuilder` crea el builder con tres argumentos:
 * - `context`: necesario para localizar el sistema de archivos
 * - `ChatDatabase::class.java`: la clase de la base de datos (generada por KSP)
 * - Nombre del archivo: el nombre que tendrá el archivo SQLite en disco
 *
 * El builder no crea la BD inmediatamente; la BD real se crea cuando se llama
 * a `.build()` en `getRoomDatabase()` (definido en ChatDatabase.kt).
 *
 * @param context El Context de Android (pasado como Any? desde commonMain).
 * @return Builder configurado para crear la ChatDatabase en Android.
 */
actual fun instantiateDatabaseBuilder(context: Any?): androidx.room.RoomDatabase.Builder<ChatDatabase> {
    // `!!` = "non-null assertion": si androidContext es null, lanza NullPointerException.
    // Aquí es aceptable porque sin Context en Android es imposible crear la BD.
    val androidContext = context as? Context
    return Room.databaseBuilder(
        androidContext!!,
        ChatDatabase::class.java,
        "caretrack_chat_database"  // Nombre del archivo SQLite
    )
}

// TODO: Reemplazar el `!!` con un manejo de error más amigable, por ejemplo:
//       `requireNotNull(androidContext) { "Se requiere un Context de Android para crear la BD" }`
//       Esto da un mensaje de error más claro en caso de error de configuración.
//
// TODO: Cuando se implemente autenticación, incluir el userId en el nombre de la
//       base de datos para que cada usuario tenga su propia BD:
//       `"caretrack_chat_${userId}"`. Así se evita mezclar mensajes entre usuarios.
