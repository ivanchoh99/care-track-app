package com.app.caretrack

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import caretrack.composeapp.generated.resources.Res
import caretrack.composeapp.generated.resources.chat_empty_state
import caretrack.composeapp.generated.resources.picker_audio
import caretrack.composeapp.generated.resources.picker_document
import caretrack.composeapp.generated.resources.picker_image
import caretrack.composeapp.generated.resources.preview_cancel
import caretrack.composeapp.generated.resources.preview_send
import caretrack.composeapp.generated.resources.preview_title
import coil3.compose.AsyncImage
import com.app.caretrack.chat.ChatInputBar
import com.app.caretrack.chat.ChatRepository
import com.app.caretrack.chat.ChatViewModel
import com.app.caretrack.chat.MessageItem
import com.app.caretrack.chat.MessageType
import com.app.caretrack.chat.UiState
import com.app.caretrack.media.audio.rememberAudioPlayer
import com.app.caretrack.media.audio.rememberAudioRecorder
import com.app.caretrack.common.AppLogger
import com.app.caretrack.common.rememberPermissionLauncher
import com.app.caretrack.common.checkInitialAudioPermission
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.extension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

@Composable
fun App(repository: ChatRepository) {
    val chatViewModel: ChatViewModel = viewModel { ChatViewModel(repository) }
    val uiState by chatViewModel.uiState.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val player = rememberAudioPlayer()
    val recorder = rememberAudioRecorder()

    var currentRecordName by remember { mutableStateOf("") }
    var hasAudioPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberPermissionLauncher(
        onResult = { isGranted ->
            hasAudioPermission = isGranted
        }
    )

    // Verificar estado inicial del permiso de audio
    LaunchedEffect(Unit) {
        hasAudioPermission = checkInitialAudioPermission()
    }

    // Imagen se envía directamente sin confirmación (selección directa)
    val imageLauncher = rememberFilePickerLauncher(
        type = PickerType.Image,
        title = stringResource(Res.string.picker_image)
    ) { file ->
        file?.let {
            scope.launch(Dispatchers.IO) {
                val bytes = it.readBytes()
                chatViewModel.processAndSendFile(
                    fileName = it.name,
                    extension = it.extension,
                    type = MessageType.IMAGE,
                    fileBytes = bytes
                )
            }
        }
    }

    val audioLauncher = rememberFilePickerLauncher(
        type = PickerType.File(extensions = ChatViewModel.VALID_AUDIO_EXT),
        title = stringResource(Res.string.picker_audio)
    ) { file ->
        file?.let {
            scope.launch(Dispatchers.IO) {
                val bytes = it.readBytes()
                chatViewModel.processAndSendFile(
                    fileName = it.name,
                    extension = it.extension,
                    type = MessageType.AUDIO,
                    fileBytes = bytes
                )
            }
        }
    }

    val pdfLauncher = rememberFilePickerLauncher(
        type = PickerType.File(extensions = listOf("pdf")),
        title = stringResource(Res.string.picker_document)
    ) { file ->
        file?.let {
            scope.launch(Dispatchers.IO) {
                val bytes = it.readBytes()
                chatViewModel.processAndSendFile(
                    fileName = it.name,
                    extension = it.extension,
                    type = MessageType.DOCUMENT,
                    fileBytes = bytes
                )
            }
        }
    }

    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.safeDrawingPadding(),
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    ChatInputBar(
                        onSendMessage = { text ->
                            chatViewModel.sendMessage(text)
                            scope.launch {
                                val current = uiState
                                if (current is UiState.Success) {
                                    listState.animateScrollToItem(current.data.size - 1)
                                }
                            }
                        },
                        onAttachmentSelected = { type ->
                            when (type) {
                                MessageType.IMAGE -> imageLauncher.launch()
                                MessageType.DOCUMENT -> pdfLauncher.launch()
                                MessageType.AUDIO -> audioLauncher.launch()
                                else -> {}
                            }
                        },
                        onVoiceNoteStart = {
                            if (hasAudioPermission) {
                                try {
                                    currentRecordName =
                                        "nota_voz_${Clock.System.now().toEpochMilliseconds()}.m4a"
                                    recorder.startRecording(currentRecordName)
                                } catch (e: Exception) {
                                    AppLogger.e("App", "Error al iniciar grabación: ${e.message}")
                                    hasAudioPermission = false
                                    permissionLauncher.launch()
                                }
                            } else {
                                permissionLauncher.launch()
                            }
                        },
                        onVoiceNoteEnd = {
                            if (hasAudioPermission) {
                                val savedPath = recorder.stopRecording()
                                chatViewModel.processAndSendFile(
                                    fileName = currentRecordName,
                                    extension = "m4a",
                                    type = MessageType.AUDIO,
                                    filePath = savedPath
                                )
                                scope.launch {
                                    val current = uiState
                                    if (current is UiState.Success && current.data.isNotEmpty()) {
                                        listState.animateScrollToItem(current.data.size - 1)
                                    }
                                }
                            }
                        }
                    )
                }
            ) { paddingValues ->
                when (val state = uiState) {
                    is UiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(paddingValues),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    is UiState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(paddingValues),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    is UiState.Success -> {
                        if (state.data.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(paddingValues),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(Res.string.chat_empty_state),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(items = state.data, key = { it.id }) { message ->
                                    MessageItem(
                                        message = message,
                                        player = player,
                                        onDelete = { chatViewModel.deleteMessage(it) },
                                        onRetry = { chatViewModel.retryMessage(it) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
