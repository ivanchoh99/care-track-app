package com.app.caretrack.chat

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow

// =============================================================================
// CAPA DE BASE DE DATOS — ROOM (Data Access Object + Database)
// =============================================================================
// Room es la librería ORM (Object-Relational Mapping) de Jetpack.
// Convierte clases Kotlin en tablas SQLite usando anotaciones, sin necesidad
// de escribir SQL manualmente para las operaciones básicas.
//
// Componentes de Room en este proyecto:
//   @Dao     → ChatDao: define las operaciones de la tabla (queries SQL)
//   @Database → ChatDatabase: punto de acceso único a la base de datos
//   @Entity  → MessageEntity (en MessageEntity.kt): define la tabla
//
// Room usa KSP (Kotlin Symbol Processing) para generar el código de acceso
// a la BD en tiempo de compilación. Por eso en build.gradle hay:
//   `ksp(libs.room.compiler)`
//
// BundledSQLiteDriver: en lugar de la implementación de SQLite del sistema
// Android (que varía por versión), usa una versión de SQLite empaquetada
// con la app. Esto garantiza comportamiento consistente en todos los dispositivos
// y permite también el soporte futuro para iOS en Room.
// =============================================================================

/**
 * Data Access Object (DAO) del chat.
 *
 * Un `interface` DAO define los métodos de acceso a la base de datos.
 * Room genera la implementación automáticamente en tiempo de compilación.
 *
 * Reglas clave de Room:
 * - Funciones que leen datos → deben ser `suspend fun` o devolver `Flow`
 * - Funciones que escriben datos → deben ser `suspend fun`
 * - `Flow<T>` → emite los datos y re-emite automáticamente cuando cambian en la BD
 */
@Dao
interface ChatDao {

    /**
     * Obtiene todos los mensajes ordenados cronológicamente.
     *
     * Devuelve un `Flow` (no un valor inmutable). Cada vez que se inserta,
     * actualiza o borra un mensaje, Room emite la nueva lista automáticamente.
     * Esto hace que la UI se actualice en tiempo real sin polling.
     *
     * `ORDER BY timestamp ASC` → mensajes más antiguos primero (como WhatsApp).
     */
    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<MessageEntity>>

    /**
     * Inserta un mensaje en la tabla. Si ya existe un mensaje con el mismo `id`,
     * Room lanzará una excepción (por el comportamiento por defecto de @Insert).
     */
    @Insert
    suspend fun insertMessage(message: MessageEntity)

    /**
     * Devuelve el número total de mensajes en la tabla.
     * Se usa para saber si hay que insertar el mensaje de bienvenida.
     */
    @Query("SELECT COUNT(*) FROM messages")
    suspend fun getMessageCount(): Int

    /** Elimina el mensaje con el `messageId` especificado. */
    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)

    /**
     * Busca un mensaje por su ID.
     * Devuelve `null` si no existe (de ahí el `MessageEntity?` con `?`).
     * Se usa en `retryMessage()` para obtener el contenido antes de reenviar.
     */
    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: String): MessageEntity?

    /**
     * Actualiza el estado de un mensaje (SENDING → SENT o FAILED).
     *
     * Solo actualiza el campo `status`, sin tocar el resto del mensaje.
     * Esto es más eficiente que hacer un SELECT + UPDATE completo.
     */
    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: String)

    /**
     * Actualiza la URL del backend para un mensaje (cuando el servidor
     * devuelve la URL pública del archivo subido).
     */
    @Query("UPDATE messages SET backendUrl = :url WHERE id = :messageId")
    suspend fun updateMessageBackendUrl(messageId: String, url: String)
}

/**
 * Clase principal de la base de datos Room.
 *
 * `@Database` define:
 * - `entities`: las tablas que contiene la BD (una por cada @Entity)
 * - `version`: número de versión del esquema. Debe incrementarse cada vez
 *   que cambies la estructura (agregar/quitar columnas, renombrar tablas, etc.)
 *
 * `abstract class` + `abstract fun chatDao()`: Room genera la implementación
 * concreta de esta clase y el DAO en tiempo de compilación.
 *
 * Singleton pattern: Room está diseñado para tener una SOLA instancia de la BD
 * por aplicación. En este proyecto se garantiza con `remember { }` en MainActivity.
 */
@Database(entities = [MessageEntity::class], version = 2)
abstract class ChatDatabase : RoomDatabase() {
    /** Provee acceso al DAO generado por Room. */
    abstract fun chatDao(): ChatDao
}

/**
 * Declara que cada plataforma proveerá la forma de construir la ChatDatabase.
 *
 * `expect fun` → En commonMain prometemos que existirá esta función.
 * Las implementaciones están en:
 *   - ChatDatabase.android.kt → usa Room.databaseBuilder con Context
 *   - ChatDatabase.ios.kt     → lanza NotImplementedError (pendiente)
 *
 * @param context El Context de Android, o null en iOS.
 * @return Builder de Room listo para configurar y construir la BD.
 */
expect fun instantiateDatabaseBuilder(context: Any?): RoomDatabase.Builder<ChatDatabase>

/**
 * Construye y configura la ChatDatabase a partir del builder de cada plataforma.
 *
 * Configuración aplicada:
 * - `setDriver(BundledSQLiteDriver())` → usa SQLite empaquetado con la app
 *   para garantizar comportamiento consistente entre dispositivos y versiones de Android.
 * - `setQueryCoroutineContext(Dispatchers.IO)` → las queries se ejecutan en el
 *   dispatcher IO (hilo de background), nunca en el hilo principal de la UI.
 * - `fallbackToDestructiveMigration(false)` → si la versión de la BD cambió sin
 *   una migración definida, lanzará excepción en lugar de borrar los datos.
 *   ¡Con `true` se perderían todos los mensajes al actualizar la app!
 *
 * @param builder Builder obtenido de [instantiateDatabaseBuilder].
 * @return La instancia de ChatDatabase lista para usar.
 */
fun getRoomDatabase(builder: RoomDatabase.Builder<ChatDatabase>): ChatDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .fallbackToDestructiveMigration(false)
        .build()
}

// TODO: Implementar migraciones de Room (Migration class) para futuros cambios
//       de esquema. Con `fallbackToDestructiveMigration(false)`, cualquier cambio
//       sin migración crasheará la app para usuarios existentes.
//       Ejemplo: cuando se agregue `userId` o `conversationId` a messages.
//
// TODO: Agregar índices a las columnas que se usan frecuentemente en queries:
//       @Entity(tableName = "messages", indices = [Index("timestamp"), Index("status")])
//       Esto acelera las búsquedas cuando haya muchos mensajes.
//
// TODO: Considerar agregar @OnConflictStrategy.REPLACE en @Insert para manejar
//       el caso (improbable) de IDs duplicados sin lanzar excepción.
