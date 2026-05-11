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

// =============================================================================
// COMPOSABLE RAÍZ DE LA APP
// =============================================================================
// Este es el punto de entrada de toda la UI de CareTrack. Es el Composable
// más alto en la jerarquía (después de MainActivity que lo invoca).
//
// Responsabilidades de App.kt:
//   1. Obtener el ViewModel y observar el estado de la UI
//   2. Gestionar los launchers de permisos y selectores de archivos
//   3. Gestionar el estado de grabación de audio
//   4. Renderizar la pantalla principal del chat (Scaffold + LazyColumn)
//   5. Manejar los tres estados de la UI: Loading, Success, Error
//
// Conceptos de Compose en este archivo:
//
// Scaffold → Estructura básica de una pantalla Material3.
//   Provee slots para TopBar, BottomBar, FloatingActionButton, Drawer, etc.
//   Gestiona automáticamente el padding para que el contenido no quede bajo las barras.
//
// LazyColumn → Equivalente al RecyclerView de Android XML.
//   Solo renderiza los ítems visibles en pantalla (lazy = perezoso).
//   Para 1000 mensajes, solo renderiza los ~10 visibles en pantalla.
//   `items(items = list, key = { it.id })` → cada ítem tiene una clave única
//   para que Compose recicle eficientemente y anime los cambios.
//
// collectAsStateWithLifecycle → Observa un StateFlow respetando el ciclo de vida.
//   Deja de observar cuando la pantalla no está visible (optimización de batería).
//   Es la forma recomendada de observar Flows en Compose desde Lifecycle 2.6+.
//
// rememberCoroutineScope → Scope de coroutines ligado al ciclo de vida del Composable.
//   Se usa para lanzar animaciones de scroll (animateScrollToItem) desde callbacks.
// =============================================================================

/**
 * Composable raíz de la aplicación CareTrack.
 *
 * Recibe el [repository] como parámetro en lugar de crearlo internamente.
 * Esto es inyección de dependencias a nivel de Composable: hace la función
 * más testeable y reutilizable.
 *
 * @param repository La fuente de datos del chat, creada en MainActivity/MainViewController.
 */
@Composable
fun App(repository: ChatRepository) {
    // `viewModel { ChatViewModel(repository) }` crea el ViewModel con una lambda factory.
    // Si el ViewModel ya existe (ej. después de rotar el teléfono), devuelve el existente.
    // La lambda `{ ChatViewModel(repository) }` solo se ejecuta la primera vez.
    val chatViewModel: ChatViewModel = viewModel { ChatViewModel(repository) }

    // `collectAsStateWithLifecycle()` convierte el StateFlow del ViewModel en un State<T>
    // de Compose. Cada vez que el StateFlow emite un nuevo valor, la UI se recompone.
    // `by` usa delegación de propiedades para acceder directamente al valor sin `.value`.
    val uiState by chatViewModel.uiState.collectAsStateWithLifecycle()

    // Estado de la lista para poder hacer scroll programático (ej. al enviar mensaje)
    val listState = rememberLazyListState()

    // Scope de coroutines ligado al Composable. Para operaciones de UI asíncronas
    // como animateScrollToItem, que no pueden llamarse directamente en callbacks síncronos.
    val scope = rememberCoroutineScope()

    // AudioPlayer y AudioRecorder se crean UNA vez por sesión (gracias a `remember` interno)
    val player = rememberAudioPlayer()
    val recorder = rememberAudioRecorder()

    // Estados locales del flujo de grabación de audio:
    var currentRecordName by remember { mutableStateOf("") }      // Nombre del archivo que se está grabando
    var hasAudioPermission by remember { mutableStateOf(false) }  // ¿Tiene permiso de micrófono?
    var isRecordingRequested by remember { mutableStateOf(false) } // ¿Se intentó grabar sin permiso?
    var isRecording by remember { mutableStateOf(false) }         // ¿Está grabando ahora mismo?

    // Launcher de permisos: cuando el usuario responde al diálogo de permiso,
    // se llama `onResult` con `true` (otorgado) o `false` (denegado).
    val permissionLauncher = rememberPermissionLauncher(
        onResult = { isGranted ->
            hasAudioPermission = isGranted
            if (isGranted && isRecordingRequested) {
                isRecordingRequested = false  // Ya no es necesario; la grabación se iniciará por LaunchedEffect
            }
        }
    )

    // LaunchedEffect(hasAudioPermission, isRecordingRequested):
    // Se ejecuta cuando ALGUNA de las dos claves cambia.
    // Caso de uso: el usuario intentó grabar sin permiso → se le pidió el permiso
    // → el usuario lo otorgó → ahora arrancamos la grabación automáticamente.
    LaunchedEffect(hasAudioPermission, isRecordingRequested) {
        if (hasAudioPermission && isRecordingRequested && currentRecordName.isEmpty()) {
            try {
                currentRecordName = "nota_voz_${Clock.System.now().toEpochMilliseconds()}.m4a"
                recorder.startRecording(currentRecordName)
            } catch (e: Exception) {
                AppLogger.e("App", "Error al iniciar grabación: ${e.message}")
                isRecordingRequested = false
            }
        }
    }

    // LaunchedEffect(Unit): `Unit` como clave significa "ejecutar solo una vez al iniciar".
    // Comprueba el estado inicial del permiso de audio sin mostrar ningún diálogo.
    LaunchedEffect(Unit) {
        hasAudioPermission = checkInitialAudioPermission()
    }

    // =========================================================================
    // LAUNCHERS DE SELECCIÓN DE ARCHIVOS (File Pickers)
    // =========================================================================
    // FileKit es una librería multiplataforma para abrir el selector de archivos
    // del sistema. Cada launcher está configurado para un tipo de archivo específico.
    //
    // `scope.launch(Dispatchers.IO)` → la lectura del archivo se hace en un hilo IO
    // (leer bytes de un archivo puede ser lento y nunca debe hacerse en el hilo principal).

    // Selector de imágenes: abre la galería/explorador de archivos filtrando imágenes
    val imageLauncher = rememberFilePickerLauncher(
        type = PickerType.Image,
        title = stringResource(Res.string.picker_image)
    ) { file ->
        file?.let {  // `?.let` ejecuta el bloque solo si `file` no es null
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

    // Selector de archivos de audio: filtra por extensiones de audio compatibles
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

    // Selector de PDFs: filtra solo archivos .pdf
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

    // =========================================================================
    // ÁRBOL DE UI
    // =========================================================================
    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.safeDrawingPadding(),  // Respetar barras del sistema (notch, barra de navegación)
                contentWindowInsets = WindowInsets(0, 0, 0, 0),  // Sin insets adicionales (los maneja safeDrawingPadding)
                bottomBar = {
                    // La barra de entrada siempre visible en la parte inferior
                    ChatInputBar(
                        isRecording = isRecording,
                        onSendMessage = { text ->
                            chatViewModel.sendMessage(text)
                            // Hacer scroll hasta el último mensaje al enviar
                            scope.launch {
                                val current = uiState
                                if (current is UiState.Success) {
                                    listState.animateScrollToItem(current.data.size - 1)
                                }
                            }
                        },
                        onAttachmentSelected = { type ->
                            // Abrir el selector de archivos según el tipo seleccionado
                            when (type) {
                                MessageType.IMAGE    -> imageLauncher.launch()
                                MessageType.DOCUMENT -> pdfLauncher.launch()
                                MessageType.AUDIO    -> audioLauncher.launch()
                                else -> {}  // TEXT no tiene selector (se escribe directamente)
                            }
                        },
                        onVoiceNoteStart = {
                            if (hasAudioPermission) {
                                try {
                                    currentRecordName = "nota_voz_${Clock.System.now().toEpochMilliseconds()}.m4a"
                                    recorder.startRecording(currentRecordName)
                                    isRecording = true
                                } catch (e: Exception) {
                                    // Si la grabación falla (ej. permiso revocado en ajustes del SO),
                                    // resetear el estado de permiso y solicitarlo de nuevo
                                    AppLogger.e("App", "Error al iniciar grabación: ${e.message}")
                                    hasAudioPermission = false
                                    isRecordingRequested = true
                                    permissionLauncher.launch()
                                }
                            } else {
                                // No hay permiso → solicitar al usuario
                                isRecordingRequested = true
                                permissionLauncher.launch()
                            }
                        },
                        onVoiceNoteEnd = {
                            if (hasAudioPermission && isRecording) {
                                isRecording = false
                                val savedPath = recorder.stopRecording()
                                // Enviar el audio grabado como mensaje
                                chatViewModel.processAndSendFile(
                                    fileName = currentRecordName,
                                    extension = "m4a",
                                    type = MessageType.AUDIO,
                                    filePath = savedPath
                                )
                                // Hacer scroll al final después de enviar el audio
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
                // `paddingValues` es el padding calculado por Scaffold para que el contenido
                // no quede debajo del bottomBar. Se aplica con `.padding(paddingValues)`.

                // Renderizar según el estado actual de la UI (patrón sealed class + when)
                when (val state = uiState) {
                    // Estado de carga: mostrar spinner centrado
                    is UiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(paddingValues),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    // Estado de error: mostrar mensaje en rojo centrado
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

                    // Estado exitoso: mostrar lista de mensajes o pantalla vacía
                    is UiState.Success -> {
                        if (state.data.isEmpty()) {
                            // Chat vacío: mostrar mensaje de bienvenida/instrucciones
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
                            // Lista de mensajes con LazyColumn
                            // `key = { it.id }` es crítico para el rendimiento:
                            // Compose usa estas claves para identificar qué ítems cambiaron,
                            // agregaron o eliminaron, y animar solo esos cambios eficientemente.
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)  // 8dp entre mensajes
                            ) {
                                items(items = state.data, key = { it.id }) { message ->
                                    MessageItem(
                                        message = message,
                                        player = player,
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

// TODO: La lógica de permisos de audio (hasAudioPermission, isRecordingRequested,
//       currentRecordName) es compleja y está mezclada con la lógica de UI en App.kt.
//       Extraerla al ChatViewModel como un estado dedicado mejoraría la legibilidad
//       y la testabilidad de la pantalla.
//
// TODO: Agregar auto-scroll al final de la lista cuando llega un mensaje nuevo del bot.
//       Actualmente solo se hace scroll al enviar el propio mensaje.
//       Observar el tamaño de `state.data` con un LaunchedEffect para detectar
//       cuando llega un mensaje nuevo y hacer scroll automáticamente.
//
// TODO: Cuando se implemente autenticación, agregar una verificación del token
//       de sesión al inicio del Composable. Si no hay sesión activa, mostrar
//       la pantalla de login en lugar del chat.
//
// TODO: La lectura de bytes de archivos seleccionados (`it.readBytes()`) se hace
//       en el scope del Composable. Si el usuario sale de la pantalla mientras
//       el archivo se lee, el scope se cancela y la operación se interrumpe.
//       Considerar mover esta operación al viewModelScope para garantizar su compleción.
//
// TODO: Mostrar el nombre del archivo en una pantalla de previsualización ANTES
//       de enviarlo (especialmente para imágenes). Actualmente se envía inmediatamente
//       al seleccionar. Las strings `preview_title`, `preview_send`, `preview_cancel`
//       ya existen como recursos pero no se usan todavía.
//
// TODO: El estado `currentRecordName` es un String vacío por defecto, pero también
//       se usa como flag para evitar doble inicio de grabación en LaunchedEffect.
//       Refactorizar a un estado más explícito (sealed class o nullable String).
