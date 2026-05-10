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

// =============================================================================
// VIEWMODEL — CAPA DE PRESENTACIÓN (MVVM)
// =============================================================================
// El ViewModel es el intermediario entre la UI (Compose) y el repositorio.
//
// ¿Por qué necesitamos ViewModel si ya tenemos Repository?
//   - El ViewModel sobrevive a rotaciones de pantalla y recreaciones de la Activity.
//     Si pusieramos la lógica directamente en App.kt (Composable), los datos
//     se perderían cada vez que el usuario rota el teléfono.
//   - El ViewModel convierte el Flow de mensajes del repositorio en un StateFlow
//     que Compose puede observar eficientemente con `collectAsStateWithLifecycle`.
//   - Separa la "presentación" de los datos (qué mostrar) de la "lógica de negocio"
//     (cómo obtenerlos y procesarlos).
//
// Ciclo de vida del ViewModel:
//   Activity/Composable creado → ViewModel se obtiene (o crea si no existe)
//   Activity rotada → ViewModel SOBREVIVE (Composable nuevo, ViewModel mismo)
//   Usuario sale de la pantalla → ViewModel.onCleared() → se destruye
//
// MVVM en este proyecto:
//   Model    = MessageEntity + ChatMessage (datos)
//   View     = App.kt + MessageItem.kt + ChatInputBar.kt (Composables)
//   ViewModel = ChatViewModel (este archivo)
// =============================================================================

/**
 * ViewModel del chat. Gestiona el estado de la UI y delega operaciones al repositorio.
 *
 * `ViewModel()` (de AndroidX Lifecycle) provee:
 * - `viewModelScope`: CoroutineScope que se cancela automáticamente cuando el ViewModel
 *   se destruye, evitando memory leaks y coroutines huérfanas.
 *
 * El constructor recibe el repositorio como parámetro (inyección de dependencias manual).
 * Esto facilita las pruebas: se puede pasar un repositorio falso en los tests.
 *
 * @param repository La fuente de datos del chat.
 */
class ChatViewModel(private val repository: ChatRepository) : ViewModel() {

    companion object {
        // Re-exportar constantes del repositorio para que la UI las acceda
        // sin necesitar importar ChatRepository directamente.
        // Esto mantiene al ViewModel como única dependencia de la UI.
        val VALID_AUDIO_EXT = ChatRepository.VALID_AUDIO_EXT
        val VALID_IMAGE_EXT = ChatRepository.VALID_IMAGE_EXT
    }

    /**
     * Estado observable de la UI: la lista de mensajes envuelta en [UiState].
     *
     * Cadena de transformaciones del Flow:
     * ```
     * repository.messages (Flow<List<ChatMessage>>)
     *    ↓ .map { UiState.Success(it) }   → envuelve en estado exitoso
     *    ↓ .catch { UiState.Error(...) }   → captura errores de la BD/red
     *    ↓ .stateIn(...)                   → convierte a StateFlow para Compose
     * ```
     *
     * `StateFlow` vs `Flow`:
     * - `Flow` es "frío": solo emite cuando alguien lo observa.
     * - `StateFlow` es "caliente": siempre tiene un valor actual y lo emite
     *   inmediatamente a nuevos suscriptores.
     * - Compose necesita `StateFlow` para `collectAsStateWithLifecycle`.
     *
     * `SharingStarted.WhileSubscribed(5000)`:
     * - Activa el upstream Flow cuando hay al menos un suscriptor.
     * - Mantiene el Flow activo 5 segundos después de que el último suscriptor
     *   se va (útil para rotaciones de pantalla rápidas, evita recargar los datos).
     * - Se desactiva completamente si no hay suscriptores por más de 5 segundos.
     *
     * `initialValue = UiState.Loading`:
     * - El primer valor que emite el StateFlow antes de que lleguen datos de la BD.
     * - Hace que la UI muestre un spinner mientras Room carga los mensajes.
     */
    val uiState: StateFlow<UiState<List<ChatMessage>>> = repository.messages
        .map { UiState.Success(it) as UiState<List<ChatMessage>> }
        .catch { e -> emit(UiState.Error("Error al cargar mensajes: ${e.message}")) }
        .stateIn(
            scope = viewModelScope,                       // Se cancela con el ViewModel
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UiState.Loading
        )

    init {
        // Asegurar que el mensaje de bienvenida existe al iniciar el ViewModel.
        // `viewModelScope.launch(Dispatchers.IO)` lanza la coroutine en el hilo IO
        // (operaciones de base de datos nunca deben ejecutarse en el hilo principal).
        viewModelScope.launch(Dispatchers.IO) {
            repository.ensureWelcomeMessage()
        }
    }

    /**
     * Envía un mensaje de texto.
     *
     * El ViewModel no hace lógica de negocio: solo lanza la coroutine y delega.
     * `viewModelScope` garantiza que si el usuario sale de la pantalla mientras
     * el mensaje se envía, la coroutine NO se cancela (el ViewModel sigue vivo
     * hasta que el usuario sale definitivamente de la pantalla).
     */
    fun sendMessage(text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.sendMessage(text)
        }
    }

    /**
     * Elimina un mensaje por su ID.
     * La eliminación incluye borrar el archivo del disco (si existe).
     */
    fun deleteMessage(messageId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMessage(messageId)
        }
    }

    /**
     * Reintenta el envío de un mensaje que falló.
     * Útil cuando hay error de red temporal.
     */
    fun retryMessage(messageId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.retryMessage(messageId)
        }
    }

    /**
     * Procesa y envía un archivo multimedia (imagen, audio o PDF).
     *
     * @param fileName  Nombre del archivo.
     * @param extension Extensión del archivo (ej. "jpg", "pdf", "wav").
     * @param type      Tipo declarado: IMAGE, AUDIO o DOCUMENT.
     * @param filePath  Ruta local del archivo si ya existe en disco (ej. audio grabado).
     * @param fileBytes Contenido del archivo si se seleccionó del picker.
     */
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

// TODO: Agregar un `Factory` para el ViewModel cuando se migre a Hilt/Koin.
//       Actualmente el ViewModel se crea con `viewModel { ChatViewModel(repository) }`
//       en App.kt, pasando el repository manualmente. Con Hilt:
//       `@HiltViewModel class ChatViewModel @Inject constructor(val repository: ChatRepository)`
//
// TODO: Considerar mover la lógica de permisos de audio (actualmente en App.kt)
//       al ViewModel. Los permisos son estado de la lógica de la app, no de la UI.
//       Esto haría App.kt más simple y el ViewModel más completo.
//
// TODO: Exponer un estado de "isLoading" para operaciones específicas (ej. enviando
//       archivo grande). Actualmente la UI solo muestra el estado del mensaje
//       individual (SENDING), pero no hay feedback global de "subiendo archivo...".
