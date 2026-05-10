package com.app.caretrack.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import caretrack.composeapp.generated.resources.Res
import caretrack.composeapp.generated.resources.action_attach
import caretrack.composeapp.generated.resources.action_record
import caretrack.composeapp.generated.resources.action_send
import caretrack.composeapp.generated.resources.add_24px
import caretrack.composeapp.generated.resources.attachment_audio
import caretrack.composeapp.generated.resources.attachment_image
import caretrack.composeapp.generated.resources.attachment_pdf
import caretrack.composeapp.generated.resources.audio_file_24px
import caretrack.composeapp.generated.resources.chat_input_placeholder
import caretrack.composeapp.generated.resources.close_24px
import caretrack.composeapp.generated.resources.image_24px
import caretrack.composeapp.generated.resources.mic_24px
import caretrack.composeapp.generated.resources.picture_as_pdf_24px
import caretrack.composeapp.generated.resources.send_24px
import caretrack.composeapp.generated.resources.stop_circle_24px
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

// =============================================================================
// BARRA DE ENTRADA DEL CHAT
// =============================================================================
// Este archivo contiene tres Composables relacionados con la entrada de mensajes:
//
//   ChatInputBar    → La barra completa (campo de texto + botones)
//   AttachmentMenu  → El menú que aparece al pulsar "+" (imagen/PDF/audio)
//   AttachmentItem  → Un ítem individual del menú de adjuntos
//
// Conceptos de Compose que aparecen aquí:
//
// Estado en Compose (`remember` + `mutableStateOf`):
//   - Compose es "declarativo": la UI se define como una función del estado.
//   - `remember { mutableStateOf("") }` crea una variable de estado que,
//     cuando cambia, hace que Compose redibuje solo los Composables que la usan.
//   - `var text by remember { mutableStateOf("") }` usa delegación de propiedades
//     (keyword `by`) para simplificar: text = valor, en lugar de text.value = valor.
//
// Recomposición:
//   Cuando `textState` o `showMenu` cambian, Compose llama a ChatInputBar() de nuevo
//   (recomposición). Solo se redibujan los Composables afectados, no toda la pantalla.
//
// Hoisting de estado (State Hoisting):
//   El estado del texto se mantiene dentro de ChatInputBar (`textState`), pero
//   el texto en sí se SUBE (hoisted) a App.kt mediante el callback `onSendMessage`.
//   Esto separa "qué mostrar" (ChatInputBar) de "qué hacer con el dato" (App.kt).
// =============================================================================

/**
 * Barra de entrada de mensajes ubicada en la parte inferior de la pantalla.
 *
 * Composable principal que incluye:
 * - Campo de texto multilínea (hasta 4 líneas)
 * - Botón "+" para abrir el menú de adjuntos
 * - Botón dinámico: Enviar (cuando hay texto) o Micrófono (cuando está vacío)
 * - Menú desplegable de adjuntos (imagen, PDF, audio)
 *
 * @param isRecording          `true` cuando se está grabando una nota de voz.
 *                             Cambia el ícono del botón de micrófono a "detener".
 * @param onSendMessage        Callback llamado cuando el usuario presiona Enviar.
 *                             Recibe el texto del campo de entrada.
 * @param onAttachmentSelected Callback cuando el usuario selecciona un tipo de adjunto.
 *                             Recibe el [MessageType] correspondiente.
 * @param onVoiceNoteStart     Callback cuando el usuario pulsa el botón de micrófono.
 * @param onVoiceNoteEnd       Callback cuando el usuario pulsa detener grabación.
 */
@Composable
fun ChatInputBar(
    isRecording: Boolean = false,
    onSendMessage: (String) -> Unit,
    onAttachmentSelected: (MessageType) -> Unit,
    onVoiceNoteStart: () -> Unit,
    onVoiceNoteEnd: () -> Unit
) {
    // Estado local del texto en el campo de entrada.
    // `by` hace que `textState` sea directamente el String (no MutableState<String>)
    var textState by remember { mutableStateOf("") }

    // Estado local que controla si el menú de adjuntos está visible
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // El menú de adjuntos solo se dibuja cuando showMenu = true.
        // En Compose, la forma de mostrar/ocultar algo es con un `if`.
        // NO hay un `visibility = GONE` como en XML. Si no está en el `if`, no existe.
        if (showMenu) {
            AttachmentMenu(
                onSelect = { type ->
                    onAttachmentSelected(type)
                    showMenu = false  // Cerrar el menú después de seleccionar
                }
            )
        }

        // Surface agrega sombra y el fondo redondeado a la barra de entrada.
        // `tonalElevation` da una elevación sutil de color (esquema Material3).
        // `shadowElevation` agrega la sombra física.
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),           // Esquinas muy redondeadas = píldora
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón de adjuntos: alterna entre "+" y "✕" según showMenu
                IconButton(onClick = { showMenu = !showMenu }) {
                    Icon(
                        painter = painterResource(
                            if (showMenu) Res.drawable.close_24px else Res.drawable.add_24px
                        ),
                        contentDescription = stringResource(Res.string.action_attach),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Campo de texto sin bordes ni fondo propio.
                // `Modifier.weight(1f)` hace que ocupe todo el espacio horizontal disponible
                // después de los botones de los extremos. Es equivalente a `layout_weight` en XML.
                // `TextFieldDefaults.colors(... Color.Transparent ...)` elimina el fondo gris
                // por defecto y el indicador de enfoque (línea subrayada) del TextField.
                TextField(
                    value = textState,
                    onValueChange = { textState = it },  // Actualizar estado en cada pulsación
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(Res.string.chat_input_placeholder)) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    maxLines = 4  // Permite hasta 4 líneas antes de hacer scroll interno
                )

                // Botón dinámico: cambia según si hay texto o si se está grabando.
                // Este patrón de "botón contextual" es común en apps de mensajería.
                if (textState.isNotBlank()) {
                    // Hay texto → mostrar botón de Enviar
                    IconButton(
                        onClick = {
                            onSendMessage(textState)
                            textState = ""  // Limpiar el campo después de enviar
                        }
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.send_24px),
                            contentDescription = stringResource(Res.string.action_send),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    // Sin texto → mostrar botón de Micrófono (o Detener si está grabando)
                    IconButton(
                        onClick = {
                            if (!isRecording) {
                                onVoiceNoteStart()
                            } else {
                                onVoiceNoteEnd()
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(
                                if (isRecording) Res.drawable.stop_circle_24px else Res.drawable.mic_24px
                            ),
                            contentDescription = stringResource(Res.string.action_record),
                            // El ícono de grabación se pone rojo para indicar actividad
                            tint = if (isRecording) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Menú desplegable para seleccionar el tipo de adjunto.
 *
 * Aparece encima de la barra de texto cuando el usuario pulsa "+".
 * Contiene tres opciones: Imagen, PDF y Audio.
 *
 * @param onSelect Callback con el [MessageType] seleccionado.
 */
@Composable
fun AttachmentMenu(onSelect: (MessageType) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 8.dp  // Mayor elevación que la barra → aparece "encima"
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly  // Distribuir uniformemente
        ) {
            AttachmentItem(
                iconRes = Res.drawable.image_24px,
                label = stringResource(Res.string.attachment_image),
                color = Color(0xFF4CAF50)  // Verde Material
            ) { onSelect(MessageType.IMAGE) }

            AttachmentItem(
                iconRes = Res.drawable.picture_as_pdf_24px,
                label = stringResource(Res.string.attachment_pdf),
                color = Color(0xFFF44336)  // Rojo Material
            ) { onSelect(MessageType.DOCUMENT) }

            AttachmentItem(
                iconRes = Res.drawable.audio_file_24px,
                label = stringResource(Res.string.attachment_audio),
                color = Color(0xFF2196F3)  // Azul Material
            ) { onSelect(MessageType.AUDIO) }
        }
    }
}

/**
 * Ítem individual del menú de adjuntos: ícono circular + etiqueta de texto.
 *
 * @param iconRes Recurso del ícono vectorial a mostrar.
 * @param label   Texto debajo del ícono.
 * @param color   Color principal del ícono y del fondo circular.
 * @param onClick Callback al pulsar el ícono.
 */
@Composable
fun AttachmentItem(
    iconRes: org.jetbrains.compose.resources.DrawableResource,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    // `Column` con `horizontalAlignment = Center` centra el ícono y el texto
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            // `color.copy(alpha = 0.1f)` toma el mismo color pero con 10% de opacidad
            // para el fondo circular semitransparente del ícono
            modifier = Modifier.background(color.copy(alpha = 0.1f), CircleShape)
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        // Etiqueta debajo del ícono con tipografía pequeña de Material3
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

// TODO: Agregar un indicador visual de nivel de grabación (ej. amplitud del micrófono)
//       mientras se graba la nota de voz. Actualmente solo cambia el color del ícono.
//       Esto mejoraría el feedback al usuario de que el audio se está capturando.
//
// TODO: Implementar "mantener pulsado para grabar" (push-to-talk) como alternativa
//       al tap-to-start/tap-to-stop. WhatsApp y Telegram usan este patrón que
//       es más intuitivo para notas de voz cortas.
//
// TODO: El menú de adjuntos usa colores hardcodeados (0xFF4CAF50, etc.). Migrar
//       al sistema de colores de MaterialTheme para soportar correctamente el
//       modo oscuro (dark mode).
//
// TODO: Los colores de los íconos en AttachmentMenu no respetan el tema de
//       la app. Definirlos en el ColorScheme del MaterialTheme para que el
//       diseñador pueda cambiarlos desde un solo lugar.
//
// TODO: Agregar soporte para pegar imágenes desde el portapapeles (clipboard)
//       directamente en el campo de texto. Es una funcionalidad esperada en apps
//       de chat modernas.
