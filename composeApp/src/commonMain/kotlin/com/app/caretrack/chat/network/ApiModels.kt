package com.app.caretrack.chat.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SendMessageRequest(
    val content: String,
    val type: String,
    @SerialName("file_name")
    val fileName: String? = null,
    val extension: String? = null,
    @SerialName("file_url")
    val fileUrl: String? = null
)

@Serializable
data class ApiResponse(
    val id: String,
    val content: String,
    val type: String,
    @SerialName("file_url")
    val fileUrl: String? = null,
    @SerialName("file_name")
    val fileName: String? = null,
    val error: String? = null
)

@Serializable
data class WebSocketMessage(
    val type: String,
    val content: String,
    @SerialName("message_id")
    val messageId: String? = null,
    @SerialName("token_index")
    val tokenIndex: Int? = null,
    val done: Boolean = false,
    val error: String? = null
)

@Serializable
data class HealthResponse(
    val status: String,
    val version: String? = null
)
