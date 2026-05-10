package com.app.caretrack.chat.network

import com.app.caretrack.auth.model.AuthResponse
import com.app.caretrack.auth.model.LoginRequest
import com.app.caretrack.auth.model.UserModel
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.send
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

// =============================================================================
// CAPA DE RED — KTOR HTTP CLIENT
// =============================================================================
// Ktor es el cliente HTTP/WebSocket multiplataforma de JetBrains, diseñado
// específicamente para KMP. Funciona en Android, iOS, Desktop y Web.
//
// ¿Por qué Ktor y no Retrofit?
// → Retrofit es solo para JVM/Android. Ktor funciona en todas las plataformas.
// → Ktor usa coroutines nativas (suspend fun) en lugar de callbacks o RxJava.
// → Se configura con DSL (Domain Specific Language) de Kotlin, más expresivo.
//
// Plugins de Ktor usados:
//   ContentNegotiation → serializa/deserializa JSON automáticamente
//   WebSockets         → soporta conexiones WebSocket
//
// Esta clase es responsable SOLO de la comunicación de red (HTTP + WebSocket).
// La lógica de negocio está en ChatRepository.
// =============================================================================

/**
 * Cliente de red para comunicarse con el servidor de CareTrack.
 *
 * Gestiona:
 * - Peticiones HTTP REST (enviar mensajes, health check)
 * - Conexiones WebSocket (recibir respuestas del bot en tiempo real)
 *
 * @param baseUrl   URL base del servidor. Por defecto `10.0.2.2` es la IP del
 *                  host desde dentro del emulador Android (equivalente a `localhost`
 *                  cuando el servidor corre en tu máquina de desarrollo).
 * @param jsonConfig Configuración del serializador JSON con opciones de lenidad.
 */
class ApiService(
    val baseUrl: String = "http://10.0.2.2:8080",
    val jsonConfig: Json = Json {
        // ignoreUnknownKeys: si el servidor envía campos nuevos que la app no conoce,
        // los ignora en lugar de lanzar excepción. Útil para evolucionar la API.
        ignoreUnknownKeys = true

        // isLenient: acepta JSON "no estricto" (ej. sin comillas en claves, comentarios).
        // Útil para servidores que no generan JSON 100% válido.
        isLenient = true

        // encodeDefaults: incluye en el JSON los campos con valores por defecto.
        // Sin esto, campos como `done = false` o `fileName = null` no se enviarían.
        encodeDefaults = true
    }
) {
    /**
     * Cliente HTTP de Ktor configurado con plugins.
     *
     * El DSL `HttpClient { install(...) }` configura el cliente con plugins:
     * - `ContentNegotiation` + `json(jsonConfig)` → maneja automáticamente la
     *   serialización/deserialización de data classes con @Serializable.
     * - `WebSockets` → habilita el soporte para conexiones WebSocket.
     */
    val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(jsonConfig)
        }
        install(WebSockets)
    }

    /**
     * Verifica si el servidor está activo y respondiendo.
     *
     * `runCatching { }` ejecuta el bloque y captura cualquier excepción,
     * devolviendo un `Result<T>`. Es equivalente a un try-catch pero más idiomático
     * en Kotlin y permite encadenar operaciones con `.onSuccess`, `.onFailure`.
     *
     * @return `Result.success(HealthResponse)` si el servidor responde,
     *         `Result.failure(exception)` si hay error de red.
     */
    suspend fun healthCheck(): Result<HealthResponse> = runCatching {
        httpClient.get("$baseUrl/health").body<HealthResponse>()
    }

    private val _accessToken = MutableStateFlow<String?>(null)
    val accessToken: StateFlow<String?> = _accessToken.asStateFlow()

    fun setAccessToken(token: String?) {
        _accessToken.value = token
    }

    suspend fun login(request: LoginRequest): Result<AuthResponse> = runCatching {
        val response = httpClient.post("$baseUrl/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<AuthResponse>()
        _accessToken.value = response.tokens.accessToken
        response
    }

    suspend fun logout(): Result<Unit> = runCatching {
        _accessToken.value?.let { token ->
            httpClient.post("$baseUrl/api/auth/logout") {
                header("Authorization", "Bearer $token")
            }
        }
        _accessToken.value = null
    }

    suspend fun refreshToken(refreshToken: String): Result<AuthResponse> = runCatching {
        val response = httpClient.post("$baseUrl/api/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("refreshToken" to refreshToken))
        }.body<AuthResponse>()
        _accessToken.value = response.tokens.accessToken
        response
    }

    suspend fun getCurrentUser(token: String): Result<UserModel> = runCatching {
        httpClient.get("$baseUrl/api/users/me") {
            header("Authorization", "Bearer $token")
        }.body<UserModel>()
    }

    /**
     * Envía un mensaje (texto o archivo) al servidor.
     *
     * `suspend fun` → esta función puede suspenderse en la petición HTTP
     * sin bloquear el hilo. Cuando el servidor responde, la ejecución continúa.
     *
     * @param request El cuerpo del request con el contenido del mensaje.
     * @return `Result.success(ApiResponse)` con la respuesta del servidor,
     *         `Result.failure(exception)` si falla la red o el servidor devuelve error HTTP.
     */
    suspend fun sendMessage(request: SendMessageRequest): Result<ApiResponse> = runCatching {
        httpClient.post("$baseUrl/api/chat/message") {
            contentType(ContentType.Application.Json)  // Header: Content-Type: application/json
            setBody(request)  // Serializa SendMessageRequest a JSON automáticamente
        }.body<ApiResponse>()
    }

    /**
     * Establece una conexión WebSocket con el servidor.
     *
     * `webSocketSession()` abre la conexión y devuelve un [WebSocketSession]
     * que permite enviar y recibir frames (mensajes WebSocket).
     *
     * WebSocket vs HTTP:
     * - HTTP → request/response: el cliente pregunta, el servidor responde, se cierra.
     * - WebSocket → canal bidireccional persistente: tanto cliente como servidor
     *   pueden enviar mensajes en cualquier momento sin esperar a que el otro pregunte.
     *
     * @return Sesión WebSocket activa.
     */
    suspend fun connectWebSocket(): WebSocketSession {
        return httpClient.webSocketSession("$baseUrl/ws/chat")
    }

    /**
     * Envía un mensaje a través de una sesión WebSocket activa.
     *
     * Serializa el [WebSocketMessage] a JSON y lo envía como un frame de texto.
     *
     * @param session La sesión WebSocket activa obtenida de [connectWebSocket].
     * @param message El mensaje a enviar.
     */
    suspend fun sendWsMessage(session: WebSocketSession, message: WebSocketMessage) {
        val text = jsonConfig.encodeToString(WebSocketMessage.serializer(), message)
        session.send(text)  // Envía el JSON como un frame WebSocket de texto
    }

    /**
     * Cierra la sesión WebSocket de forma ordenada.
     *
     * Es importante cerrar el WebSocket correctamente (con un "close frame")
     * para que el servidor sepa que el cliente se desconectó intencionalmente,
     * a diferencia de una desconexión por error de red.
     */
    suspend fun disconnect(session: WebSocketSession) {
        session.close()
    }
}

// TODO: `httpClient` es `val` público. Debería ser `private val` para encapsular
//       la implementación. Actualmente ChatWebSocket accede a él directamente,
//       pero sería mejor que ApiService exponga métodos de alto nivel en su lugar.
//
// TODO: La URL base `"http://10.0.2.2:8080"` está hardcodeada. Para un proyecto
//       real, debería configurarse mediante un archivo de configuración o flavor
//       de build (debug/release/staging). En producción la URL será diferente.
//
// TODO: Agregar el plugin `HttpTimeout` de Ktor para configurar timeouts:
//       `install(HttpTimeout) { requestTimeoutMillis = 30_000 }`.
//       Sin timeout, una red lenta puede dejar la app "colgada" indefinidamente.
//
// TODO: Cuando se implemente autenticación, agregar el plugin `Auth` de Ktor
//       o un interceptor que añada el header `Authorization: Bearer <token>`
//       a todas las peticiones automáticamente.
//
// TODO: Agregar logging de red en modo debug con el plugin `Logging` de Ktor:
//       `install(Logging) { level = LogLevel.BODY }`.
//       Invaluable para depurar problemas de API.
