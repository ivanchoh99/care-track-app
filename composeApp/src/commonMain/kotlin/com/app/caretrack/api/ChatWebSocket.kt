package com.app.caretrack.api

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

class ChatWebSocket(
    private val apiService: ApiService,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private var retries = 0
    private val maxRetries = 5

    private val _messages = MutableSharedFlow<WebSocketMessage>(replay = 0, extraBufferCapacity = 64)
    val messages: SharedFlow<WebSocketMessage> = _messages.asSharedFlow()

    private val _connectionState = MutableSharedFlow<ConnectionState>(replay = 1, extraBufferCapacity = 1)
    val connectionState: SharedFlow<ConnectionState> = _connectionState.asSharedFlow()

    enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED }

    fun connect() {
        scope.launch {
            _connectionState.emit(ConnectionState.CONNECTING)
            try {
                val session = apiService.connectWebSocket()
                _connectionState.emit(ConnectionState.CONNECTED)
                retries = 0

                session.incoming.consumeAsFlow().collect { frame ->
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        try {
                            val message = apiService.jsonConfig.decodeFromString<WebSocketMessage>(text)
                            _messages.emit(message)
                        } catch (e: Exception) {
                            _messages.emit(WebSocketMessage("error", "Parse error: ${e.message}"))
                        }
                    }
                }
            } catch (e: Exception) {
                _connectionState.emit(ConnectionState.DISCONNECTED)
                if (retries < maxRetries) {
                    retries++
                    delay((1000L * retries).coerceAtMost(10000L))
                    connect()
                }
            }
        }
    }

    suspend fun send(message: WebSocketMessage) {
        val session = try {
            apiService.connectWebSocket()
        } catch (_: Exception) { null }

        session?.let {
            apiService.sendWsMessage(it, message)
            apiService.disconnect(it)
        }
    }

    fun disconnect() {
        scope.launch {
            _connectionState.emit(ConnectionState.DISCONNECTED)
        }
    }
}
