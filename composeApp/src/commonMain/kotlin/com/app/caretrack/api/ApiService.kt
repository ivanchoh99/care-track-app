package com.app.caretrack.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.send
import kotlinx.serialization.json.Json

class ApiService(
    val baseUrl: String = "http://10.0.2.2:8080",
    val jsonConfig: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
) {
    val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(jsonConfig)
        }
        install(WebSockets)
    }

    suspend fun healthCheck(): Result<HealthResponse> = runCatching {
        httpClient.get("$baseUrl/health").body<HealthResponse>()
    }

    suspend fun sendMessage(request: SendMessageRequest): Result<ApiResponse> = runCatching {
        httpClient.post("$baseUrl/api/chat/message") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<ApiResponse>()
    }

    suspend fun connectWebSocket(): WebSocketSession {
        return httpClient.webSocketSession("$baseUrl/ws/chat")
    }

    suspend fun sendWsMessage(session: WebSocketSession, message: WebSocketMessage) {
        val text = jsonConfig.encodeToString(WebSocketMessage.serializer(), message)
        session.send(text)
    }

    suspend fun disconnect(session: WebSocketSession) {
        session.close()
    }
}
