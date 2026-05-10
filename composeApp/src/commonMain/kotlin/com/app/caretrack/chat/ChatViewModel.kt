package com.app.caretrack.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(private val repository: ChatRepository) : ViewModel() {

    companion object {
        val VALID_AUDIO_EXT = ChatRepository.VALID_AUDIO_EXT
        val VALID_IMAGE_EXT = ChatRepository.VALID_IMAGE_EXT
    }

    val uiState: StateFlow<UiState<List<ChatMessage>>> = repository.messages
        .map { UiState.Success(it) as UiState<List<ChatMessage>> }
        .catch { e -> emit(UiState.Error("Error al cargar mensajes: ${e.message}")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UiState.Loading
        )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.ensureWelcomeMessage()
        }
    }

    fun sendMessage(text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.sendMessage(text)
        }
    }

    fun processAndSendFile(
        fileName: String,
        extension: String?,
        type: MessageType,
        filePath: String? = null,
        fileBytes: ByteArray? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.processAndSendFile(
                fileName = fileName,
                extension = extension,
                type = type,
                filePath = filePath,
                fileBytes = fileBytes
            )
        }
    }
}
