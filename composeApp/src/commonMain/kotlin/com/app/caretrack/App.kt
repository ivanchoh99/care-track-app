package com.app.caretrack

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.caretrack.chat.ChatDao
import com.app.caretrack.chat.ChatInputBar
import com.app.caretrack.chat.ChatViewModel
import com.app.caretrack.chat.MessageItem
import com.app.caretrack.chat.MessageType
import com.app.caretrack.chat.rememberAudioPlayer
import com.app.caretrack.chat.rememberAudioRecorder
import com.app.caretrack.chat.rememberPermissionLauncher
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.extension
import kotlinx.coroutines.launch
import kotlin.time.Clock

@Composable
fun App(dao: ChatDao) {

    val chatViewModel: ChatViewModel = viewModel { ChatViewModel(dao) }
    val messages by chatViewModel.messages.collectAsStateWithLifecycle()
    val recorder = rememberAudioRecorder()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val player = rememberAudioPlayer()
    var currentRecordName by remember { mutableStateOf("") }

    // --- LÓGICA DE PERMISOS ---
    var hasAudioPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberPermissionLauncher { isGranted ->
        hasAudioPermission = isGranted
    }

    // --- SELECTORES DE ARCHIVOS (FILEKIT) ---

    // Selector de Imágenes
    val imageLauncher = rememberFilePickerLauncher(
        type = PickerType.Image,
        title = "Seleccionar Imagen"
    ) { file ->
        // USAMOS LA NUEVA FUNCIÓN UNIFICADA
        file?.let { chatViewModel.processAndSendFile(it.name, it.extension, MessageType.IMAGE) }
    }

// Selector de Audio
    val audioLauncher = rememberFilePickerLauncher(
        type = PickerType.File(extensions = ChatViewModel.VALID_AUDIO_EXT),
        title = "Seleccionar Audio"
    ) { file ->
        file?.let {
            scope.launch {
                // AQUÍ ES DONDE RESCATAMOS LA INFORMACIÓN ANTES DE QUE FILEKIT LA BORRE
                val bytes = it.readBytes()

                chatViewModel.processAndSendFile(
                    fileName = it.name,
                    extension = it.extension,
                    type = MessageType.AUDIO,
                    filePath = bytes // Lo enviamos a tu modelo
                )
            }
        }
    }

// Selector de PDF
    val pdfLauncher = rememberFilePickerLauncher(
        type = PickerType.File(extensions = listOf("pdf")),
        title = "Seleccionar PDF"
    ) { file ->
        file?.let { chatViewModel.processAndSendFile(it.name, it.extension, MessageType.DOCUMENT) }
    }

    MaterialTheme {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(), // Soluciona el solapamiento con la barra de estado
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            bottomBar = {
                ChatInputBar(
                    onSendMessage = { text ->
                        chatViewModel.sendMessage(text)
                        scope.launch {
                            listState.animateScrollToItem(messages.size - 1)
                        }
                    },
                    onAttachmentSelected = { type ->
                        // Disparamos el launcher correspondiente según la categoría
                        when (type) {
                            MessageType.IMAGE -> imageLauncher.launch()
                            MessageType.DOCUMENT -> pdfLauncher.launch()
                            MessageType.AUDIO -> audioLauncher.launch()
                            else -> {}
                        }
                    },
                    onVoiceNoteStart = {
                        if (hasAudioPermission) {
                            // Generamos un nombre único con el timestamp actual
                            currentRecordName =
                                "nota_voz_${Clock.System.now().toEpochMilliseconds()}.m4a"
                            recorder.startRecording(currentRecordName)
                        } else {
                            permissionLauncher.launch()
                        }
                    },
                    onVoiceNoteEnd = {
                        if (hasAudioPermission) {
                            recorder.stopRecording()

                            // Usamos el nombre único que generamos al inicio
                            chatViewModel.processAndSendFile(
                                fileName = currentRecordName,
                                extension = "m4a",
                                MessageType.AUDIO
                            )

                            scope.launch {
                                if (messages.isNotEmpty()) {
                                    listState.animateScrollToItem(messages.size - 1)
                                }
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    MessageItem(message = message, player = player)
                }
            }
        }
    }
}