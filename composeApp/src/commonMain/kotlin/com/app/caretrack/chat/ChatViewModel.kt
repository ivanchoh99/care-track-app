package com.app.caretrack.chat

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel

class ChatViewModel : ViewModel() {
    val messages = mutableStateListOf(
        ChatMessage(
            content = "¡Bienvenido a CareTrack! 👋\n¿En qué puedo ayudarte hoy?",
            type = MessageType.TEXT,
            isMine = false
        )
    )

    fun sendMessage(text: String) {
        messages.add(ChatMessage(content = text, type = MessageType.TEXT, isMine = true))
        // Simulación de respuesta
        messages.add(
            ChatMessage(
                content = "Entendido, procesando: \"$text\"",
                type = MessageType.TEXT,
                isMine = false
            )
        )
    }
}