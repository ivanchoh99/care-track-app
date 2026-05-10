package com.app.caretrack.chat.network

import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch

// =============================================================================
// GESTOR DE WEBSOCKET — CONEXIÓN PERSISTENTE Y RECONEXIÓN AUTOMÁTICA
// =============================================================================
// WebSocket es un protocolo de comunicación bidireccional sobre TCP.
// Una vez establecida la conexión, tanto el cliente como el servidor pueden
// enviar mensajes en cualquier momento sin necesidad de que el otro lo pida.
//
// En CareTrack se usa para:
// → Recibir las respuestas del bot en tiempo real (streaming token por token)
// → Evitar polling (preguntar al servidor cada X segundos si hay respuesta)
//
// Patrón de diseño usado: Observable/Reactive con Kotlin Flows
//
// Flow vs LiveData vs callbacks:
// → Flow es la solución moderna de Kotlin para streams de datos asíncronos.
// → MutableSharedFlow = puede emitir desde cualquier coroutine
// → SharedFlow (inmutable) = los observadores pueden recibir sin emitir
//
// La reconexión automática sigue el patrón "exponential backoff":
// → Intento 1: esperar 1 segundo
// → Intento 2: esperar 2 segundos
// → Intento 3: esperar 3 segundos
// → ... hasta maxRetries (5), nunca más de 10 segundos de espera
// =============================================================================

/**
 * Gestiona la conexión WebSocket con el servidor y emite mensajes recibidos.
 *
 * Responsabilidades:
 * 1. Conectar al WebSocket del servidor
 * 2. Escuchar frames de texto y deserializarlos a [WebSocketMessage]
 * 3. Emitir cada mensaje a través del Flow `messages`
 * 4. Reconectar automáticamente si la conexión se pierde
 *
 * @param apiService Instancia de [ApiService] que provee el cliente Ktor.
 * @param scope      CoroutineScope en el que corren las coroutines del WebSocket.
 *                   Las coroutines viven mientras este scope esté activo.
 */
class ChatWebSocket(
    private val apiService: ApiService,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private var retries = 0
    private val maxRetries = 5  // Máximo 5 intentos antes de rendirse

    /**
     * Canal de mensajes recibidos desde el servidor.
     *
     * `MutableSharedFlow` es el emisor interno (privado, solo esta clase puede emitir).
     * - `replay = 0`          → nuevos suscriptores no reciben mensajes anteriores
     * - `extraBufferCapacity = 64` → puede almacenar hasta 64 mensajes sin suscriptores,
     *   evitando que `emit()` se suspenda si el consumidor va lento.
     */
    private val _messages = MutableSharedFlow<WebSocketMessage>(replay = 0, extraBufferCapacity = 64)

    /**
     * Flow de solo lectura expuesto al exterior.
     *
     * `.asSharedFlow()` devuelve una vista inmutable del `_messages` interno.
     * El repositorio se suscribe a este Flow para recibir mensajes del bot.
     *
     * Patrón backing property: convención en Kotlin para exponer un estado mutable
     * internamente pero inmutable externamente:
     *   private val _messages → mutable (solo esta clase emite)
     *   val messages          → inmutable (cualquiera puede observar)
     */
    val messages: SharedFlow<WebSocketMessage> = _messages.asSharedFlow()

    /**
     * Estado actual de la conexión WebSocket.
     *
     * `replay = 1` → los nuevos suscriptores reciben inmediatamente el último
     * estado emitido (saben si están conectados o no sin esperar al siguiente cambio).
     */
    private val _connectionState = MutableSharedFlow<ConnectionState>(replay = 1, extraBufferCapacity = 1)
    val connectionState: SharedFlow<ConnectionState> = _connectionState.asSharedFlow()

    /** Estados posibles de la conexión WebSocket. */
    enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED }

    /**
     * Inicia la conexión WebSocket y empieza a escuchar mensajes.
     *
     * Se llama recursivamente para implementar la reconexión automática.
     * Si la conexión se pierde (excepción), espera un tiempo creciente
     * y vuelve a llamarse hasta `maxRetries` veces.
     *
     * `scope.launch { }` lanza una nueva coroutine en el scope dado.
     * La coroutine no bloquea el hilo que llama a connect().
     */
    fun connect() {
        scope.launch {
            _connectionState.emit(ConnectionState.CONNECTING)
            try {
                // Abrir la sesión WebSocket — se suspende hasta que el servidor acepte
                val session = apiService.connectWebSocket()
                _connectionState.emit(ConnectionState.CONNECTED)
                retries = 0  // Resetear contador de reintentos al conectar exitosamente

                // `consumeAsFlow()` convierte el canal de frames en un Flow de Kotlin.
                // `.collect { }` recibe cada frame a medida que llega del servidor.
                // Este collect es un loop infinito que solo termina si la conexión se cierra.
                session.incoming.consumeAsFlow().collect { frame ->
                    // Solo procesamos frames de texto (ignoramos binarios, ping/pong, etc.)
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        try {
                            // Deserializar el JSON recibido a WebSocketMessage
                            val message = apiService.jsonConfig.decodeFromString<WebSocketMessage>(text)
                            _messages.emit(message)
                        } catch (e: Exception) {
                            // Si el JSON es inválido, emitir un mensaje de error
                            _messages.emit(WebSocketMessage("error", "Parse error: ${e.message}"))
                        }
                    }
                }
            } catch (e: Exception) {
                // La conexión falló o se perdió
                _connectionState.emit(ConnectionState.DISCONNECTED)

                if (retries < maxRetries) {
                    retries++
                    // Backoff exponencial: esperar `retries` segundos (máximo 10)
                    // coerceAtMost(10000) limita el máximo a 10 segundos
                    val waitTime = (1000L * retries).coerceAtMost(10000L)
                    delay(waitTime)
                    connect()  // Recursión: intenta conectar de nuevo
                }
                // Si se alcanzó maxRetries, la función termina (no hay más reconexión)
            }
        }
    }

    /**
     * Envía un mensaje a través del WebSocket.
     *
     * ⚠️ PROBLEMA ACTUAL: Este método abre una NUEVA sesión WebSocket para enviar
     * el mensaje y luego la cierra inmediatamente. Esto es ineficiente y puede causar
     * problemas si el servidor asocia el estado de la conversación a la sesión.
     *
     * Debería reutilizar la sesión existente que está escuchando mensajes.
     * Ver TODO abajo.
     *
     * @param message El mensaje WebSocket a enviar.
     */
    suspend fun send(message: WebSocketMessage) {
        // TODO: Esto abre una nueva conexión en lugar de reutilizar la existente.
        //       Debe refactorizarse para usar la sesión activa del método connect().
        val session = try {
            apiService.connectWebSocket()
        } catch (_: Exception) { null }

        session?.let {
            apiService.sendWsMessage(it, message)
            apiService.disconnect(it)
        }
    }

    /**
     * Marca el estado como desconectado.
     *
     * ⚠️ Este método no cierra realmente la sesión WebSocket activa (si existe).
     * Solo emite el estado DISCONNECTED al Flow, pero la coroutine de `connect()`
     * puede seguir activa en background.
     */
    fun disconnect() {
        scope.launch {
            _connectionState.emit(ConnectionState.DISCONNECTED)
        }
    }
}

// TODO: CRÍTICO — El método `send()` abre una nueva sesión WebSocket para cada
//       envío y luego la cierra. Esto es incorrecto. La arquitectura debería
//       mantener una única sesión activa (la de `connect()`) y usarla tanto para
//       enviar como para recibir. Guardar la sesión como propiedad de la clase:
//       `private var activeSession: WebSocketSession? = null`
//
// TODO: El método `disconnect()` no cancela la coroutine de `connect()`.
//       Si se llama `disconnect()` mientras la reconexión está esperando (delay),
//       el WebSocket se volverá a conectar igual. Agregar una variable `isDisconnecting`
//       o usar un Job que se pueda cancelar explícitamente.
//
// TODO: La reconexión recursiva puede causar un stack overflow teórico si falla
//       muchas veces muy rápido. Refactorizar a un loop `while` con un `Job`
//       cancelable en lugar de recursión.
//
// TODO: Agregar logging del estado de conexión para facilitar el debugging:
//       AppLogger.d("WebSocket", "Estado: ${state}, intento: $retries/$maxRetries")
//
// TODO: Cuando se implemente autenticación, el WebSocket necesitará enviar
//       el token en el header de la conexión inicial o como primer mensaje.
