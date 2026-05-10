package com.app.caretrack.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.delay
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import caretrack.composeapp.generated.resources.Res
import caretrack.composeapp.generated.resources.action_play
import caretrack.composeapp.generated.resources.action_stop
import caretrack.composeapp.generated.resources.chat_sending_status
import caretrack.composeapp.generated.resources.picture_as_pdf_24px
import caretrack.composeapp.generated.resources.play_circle_24px
import caretrack.composeapp.generated.resources.stop_circle_24px
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.app.caretrack.media.audio.AudioPlayer
import com.app.caretrack.common.AppLogger

// =============================================================================
// COMPONENTE DE BURBUJA DE MENSAJE
// =============================================================================
// Este archivo renderiza cada mensaje individual en la lista del chat.
// Soporta cuatro tipos de contenido: texto, imagen, audio y documentos PDF.
//
// Conceptos de Compose importantes en este archivo:
//
// LaunchedEffect(key) → "efecto secundario" que se ejecuta como coroutine.
//   Solo se relanza cuando `key` cambia. Úsalo para operaciones que no son
//   parte directa del renderizado: timers, actualizaciones periódicas, etc.
//
// SwipeToDismissBox → componente que detecta el gesto de deslizar para
//   acciones como "borrar". El `dismissState` trackea la posición del gesto.
//
// LocalConfiguration.current → provee información del dispositivo como el
//   ancho de pantalla, densidad y orientación.
//
// ⚠️ NOTA: Este archivo usa imports de Java (`java.io.File`, `java.text.SimpleDateFormat`,
//   `java.util.Calendar`) que NO son multiplataforma. Funcionan en Android/JVM
//   pero no compilarán para iOS. Ver TODOs al final.
// =============================================================================

/**
 * Renderiza una burbuja de mensaje individual en el chat.
 *
 * El mensaje del usuario (isMine=true) aparece a la derecha con fondo del color primario.
 * El mensaje del bot (isMine=false) aparece a la izquierda con fondo de superficie.
 *
 * Soporta eliminación con gesto deslizar-hacia-la-izquierda (SwipeToDismiss).
 *
 * @param message  El mensaje a mostrar.
 * @param player   Reproductor de audio para mensajes de tipo AUDIO (puede ser null).
 * @param onDelete Callback cuando el usuario desliza para eliminar. Recibe el messageId.
 * @param onRetry  Callback cuando el usuario pulsa el botón de reintentar. Recibe el messageId.
 */
@Composable
fun MessageItem(
    message: ChatMessage,
    player: AudioPlayer? = null,
    onDelete: ((String) -> Unit)? = null,
    onRetry: ((String) -> Unit)? = null
) {
    val isMine = message.isMine

    // LocalConfiguration provee el ancho de pantalla en dp.
    // Las burbujas tienen un ancho máximo del 70% de la pantalla,
    // igual que WhatsApp y la mayoría de apps de mensajería.
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val maxBubbleWidth = (screenWidthDp * 0.7).dp

    // Colores semánticos de Material3:
    // - Usuario: fondo primary (color principal del tema), texto onPrimary (contraste)
    // - Bot: fondo surfaceVariant (superficie secundaria), texto onSurfaceVariant
    val containerColor = if (isMine) MaterialTheme.colorScheme.primary
                         else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isMine) MaterialTheme.colorScheme.onPrimary
                       else MaterialTheme.colorScheme.onSurfaceVariant

    // Estado del gesto de deslizar para eliminar.
    // `rememberSwipeToDismissBoxState()` crea y recuerda el estado entre recomposiciones.
    val dismissState = rememberSwipeToDismissBoxState()

    // LaunchedEffect(dismissState.currentValue) se ejecuta cada vez que cambia
    // el estado del dismiss. Cuando llega a EndToStart (deslizó a la izquierda),
    // se llama onDelete y se resetea el estado visual.
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDelete?.invoke(message.id)  // El `?.` es null-safe: solo llama si onDelete != null
            dismissState.reset()
        }
    }

    // SwipeToDismissBox envuelve el contenido y detecta el gesto.
    // `enableDismissFromStartToEnd = false` → solo permite deslizar de derecha a izquierda.
    // `backgroundContent = {}` → no hay contenido de fondo visible al deslizar (sin ícono de basurero).
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {},
        enableDismissFromStartToEnd = false,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            // Los mensajes propios van a la derecha, los del bot a la izquierda
            horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
        ) {
            Surface(
                modifier = Modifier.widthIn(max = maxBubbleWidth),
                color = containerColor,
                // Forma de la burbuja: esquinas redondeadas excepto la esquina de la "cola"
                // Usuario (isMine): esquina inferior derecha plana → cola apunta al usuario (derecha)
                // Bot (isMine=false): esquina inferior izquierda plana → cola apunta al bot (izquierda)
                shape = if (isMine)
                    RoundedCornerShape(16.dp, 16.dp, 2.dp, 16.dp)   // top-left, top-right, bottom-right, bottom-left
                else
                    RoundedCornerShape(16.dp, 16.dp, 16.dp, 2.dp),
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    // Renderizar el contenido según el tipo de mensaje.
                    // `when` actúa como switch sobre el enum MessageType.
                    when (message.type) {
                        MessageType.TEXT -> {
                            Text(text = message.content, color = contentColor)
                        }

                        MessageType.AUDIO -> {
                            // Solo renderiza el reproductor si tenemos un AudioPlayer disponible
                            if (player != null) {
                                AudioMessageBubble(message, player)
                            }
                        }

                        MessageType.DOCUMENT -> {
                            // Verificar si el archivo PDF existe localmente
                            val pdfFile = message.filePath?.let { path ->
                                if (File(path).exists()) File(path) else null
                            }

                            // Tarjeta que muestra el ícono PDF y el nombre del archivo
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = containerColor.copy(alpha = 0.3f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Ícono de PDF en un contenedor cuadrado con fondo del color de error
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.errorContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painterResource(Res.drawable.picture_as_pdf_24px),
                                            contentDescription = "PDF",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }

                                    Column(
                                        modifier = Modifier.padding(start = 12.dp).weight(1f)
                                    ) {
                                        // Nombre del archivo (máximo 2 líneas, trunca con "…")
                                        Text(
                                            text = message.content,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = contentColor,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        // Tamaño del archivo (si está disponible)
                                        message.size?.let { size ->
                                            Text(
                                                text = size,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = contentColor.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        MessageType.IMAGE -> {
                            // Priorizar el archivo local; si no existe, usar la URL del backend
                            val imagePath = message.filePath ?: message.backendUrl
                            if (imagePath != null) {
                                // AsyncImage de Coil carga la imagen de forma asíncrona:
                                // - Si es una ruta local → la lee del disco
                                // - Si es una URL → la descarga de internet (con caché)
                                // ContentScale.Crop → recorta la imagen para llenar el espacio
                                AsyncImage(
                                    model = imagePath,
                                    contentDescription = message.content,
                                    modifier = Modifier
                                        .size(200.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                // Fallback: mostrar el nombre del archivo si no hay imagen
                                Text(message.content, color = contentColor)
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // Fila inferior: timestamp a la izquierda, estado del mensaje a la derecha
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTimestamp(message.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor.copy(alpha = 0.6f),  // Semitransparente para menor prominencia
                            fontSize = 10.sp
                        )

                        // Indicador de estado (spinner, botón de reintento, o nada si fue enviado)
                        StatusIndicator(message = message, onRetry = onRetry)
                    }
                }
            }
        }
    }
}

/**
 * Indicador del estado de envío del mensaje.
 *
 * - SENDING → spinner circular + texto "Enviando..."
 * - FAILED  → botón "↻" para reintentar el envío
 * - SENT / PENDING → no muestra nada (el mensaje llegó o está en espera)
 *
 * Es un Composable privado (solo usable dentro de este archivo).
 */
@Composable
private fun StatusIndicator(message: ChatMessage, onRetry: ((String) -> Unit)?) {
    when (message.status) {
        MessageStatus.SENDING -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // CircularProgressIndicator pequeño (12dp) para no dominar la burbuja
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.5.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    stringResource(Res.string.chat_sending_status),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        MessageStatus.FAILED -> {
            // Botón de reintento con el símbolo ↻ (flecha circular)
            IconButton(
                onClick = { onRetry?.invoke(message.id) },
                modifier = Modifier.size(20.dp)
            ) {
                Text(
                    "↻",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.error,  // Rojo para indicar error
                    textAlign = TextAlign.Center
                )
            }
        }

        // SENT y PENDING no muestran indicador (el mensaje llegó OK o está esperando)
        MessageStatus.SENT -> {}
        MessageStatus.PENDING -> {}
    }
}

/**
 * Formatea un timestamp en milisegundos a texto legible y contextual.
 *
 * Lógica de formato (igual que WhatsApp):
 * - Hoy → "14:35"
 * - Ayer → "Ayer"
 * - Esta semana (menos de 7 días) → "Lunes", "Martes", etc.
 * - Más antiguo → "15/03/24"
 *
 * Usa `Calendar` de Java (no multiplataforma). Ver TODO al final del archivo.
 *
 * @param millis Timestamp en milisegundos desde epoch (Unix timestamp).
 * @return String formateado para mostrar en la UI.
 */
private fun formatTimestamp(millis: Long): String {
    if (millis == 0L) return ""

    val messageTime = Calendar.getInstance().apply { timeInMillis = millis }

    // Crear instancias de Calendar para inicio del día de hoy y de ayer
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val yesterday = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    // Comparación temporal: ¿el mensaje es más reciente que el inicio de hoy?
    if (messageTime.after(today)) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        return timeFormat.format(Date(millis))
    }

    if (messageTime.after(yesterday)) {
        return "Ayer"
    }

    // Diferencia en días usando aritmética de milisegundos
    val daysDiff = ((today.timeInMillis - messageTime.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
    if (daysDiff < 7) {
        // "EEEE" = nombre completo del día de la semana en el locale actual
        val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
        return dayFormat.format(Date(millis))
    }

    val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
    return dateFormat.format(Date(millis))
}

/**
 * Reproductor de audio integrado en una burbuja de chat.
 *
 * Muestra:
 * - Botón Play/Stop para iniciar/detener la reproducción
 * - Barra de progreso lineal que avanza en tiempo real
 * - Nombre del archivo y contador de tiempo transcurrido
 *
 * Usa un `LaunchedEffect` con polling cada 100ms para actualizar
 * la UI con la posición actual del audio.
 *
 * @param message El mensaje de tipo AUDIO con la ruta del archivo.
 * @param player  El reproductor de audio activo.
 */
@Composable
fun AudioMessageBubble(message: ChatMessage, player: AudioPlayer) {
    // Estado local del reproductor: ¿está reproduciendo?
    var isPlaying by remember { mutableStateOf(false) }
    // Progreso de 0.0 a 1.0 para la LinearProgressIndicator
    var progress by remember { mutableStateOf(0f) }
    // Tiempo transcurrido en segundos para el contador
    var seconds by remember { mutableStateOf(0) }

    // Color del ícono y la barra de progreso según si el mensaje es del usuario o del bot
    val dynamicTintColor =
        if (message.isMine) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant

    // LaunchedEffect(isPlaying) → se ejecuta como coroutine cuando `isPlaying` cambia.
    // Cuando `isPlaying = true`: el loop actualiza la UI cada 100ms.
    // Cuando `isPlaying = false`: la coroutine se cancela (ya no hay loop).
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            if (player.isPlaying()) {
                val current = player.getCurrentPosition()
                val total = player.getDuration()
                if (total > 0) {
                    progress = current.toFloat() / total.toFloat()
                    seconds = current / 1000
                }
            } else {
                // El reproductor se detuvo (fin natural o error)
                val duration = player.getDuration()
                if (duration > 0 && progress >= 0.99f) {
                    // Fin natural: resetear UI
                    isPlaying = false
                    progress = 0f
                    seconds = 0
                } else if (!player.isPlaying() && progress > 0f) {
                    // Detuvo por otra razón: resetear igualmente
                    isPlaying = false
                    progress = 0f
                    seconds = 0
                }
            }
            delay(100)  // Actualizar 10 veces por segundo (cada 100ms)
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        // Botón Play/Stop: alterna entre reproducir y detener
        IconButton(
            onClick = {
                AppLogger.d("AudioBubble", "Botón presionado - isPlaying: $isPlaying")

                if (isPlaying) {
                    // Detener la reproducción
                    player.stopAudio()
                    isPlaying = false
                    progress = 0f
                    seconds = 0
                } else {
                    // Iniciar la reproducción:
                    // Prioridad: archivo local → URL del backend
                    val path = message.filePath
                    AppLogger.d("AudioBubble", "message.filePath: $path")

                    if (!path.isNullOrBlank()) {
                        val audioFile = File(path)
                        if (audioFile.exists()) {
                            // El archivo existe localmente → reproducir directamente
                            player.playAudio(path)
                            isPlaying = true
                        } else {
                            AppLogger.w("AudioBubble", "Archivo local NO existe: $path")
                            // Fallback: intentar con la URL del servidor
                            val fallbackPath = message.backendUrl
                            if (!fallbackPath.isNullOrBlank()) {
                                player.playAudio(fallbackPath)
                                isPlaying = true
                            } else {
                                AppLogger.e("AudioBubble", "No hay URL de backend disponible")
                            }
                        }
                    } else {
                        // No hay ruta local → intentar directamente con la URL del backend
                        val fallbackPath = message.backendUrl
                        if (!fallbackPath.isNullOrBlank()) {
                            player.playAudio(fallbackPath)
                            isPlaying = true
                        } else {
                            AppLogger.e("AudioBubble", "No se encontró archivo de audio")
                        }
                    }
                }
            }
        ) {
            Icon(
                painter = painterResource(
                    if (isPlaying) Res.drawable.stop_circle_24px else Res.drawable.play_circle_24px
                ),
                contentDescription = if (isPlaying) stringResource(Res.string.action_stop)
                                     else stringResource(Res.string.action_play),
                tint = dynamicTintColor,
                modifier = Modifier.size(32.dp)
            )
        }

        // Barra de progreso y metadatos del audio
        Column(modifier = Modifier.width(150.dp).padding(start = 8.dp)) {
            // `progress = { progress }` usa un lambda para que Compose pueda
            // leer el progreso sin recomponer toda la burbuja (solo el indicador)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = dynamicTintColor,
                trackColor = dynamicTintColor.copy(alpha = 0.3f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Nombre del archivo truncado a 12 caracteres para no desbordar
                val displayName = message.content.substringAfterLast("/").take(12)
                Text(
                    text = if (message.content.length > 12) "$displayName..." else displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = dynamicTintColor.copy(alpha = 0.8f)
                )

                // Formato MM:SS del tiempo transcurrido
                val displayMinutes = seconds / 60
                val displaySeconds = seconds % 60
                Text(
                    text = "$displayMinutes:${displaySeconds.toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.labelSmall,
                    color = dynamicTintColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// TODO: Los imports `java.io.File`, `java.text.SimpleDateFormat`, `java.util.Calendar`
//       y `java.util.Date` son específicos de Java/Android y no compilarán en iOS.
//       Para hacer este archivo verdaderamente multiplataforma:
//       - Reemplazar `java.io.File` con una función expect/actual que verifique existencia del archivo
//       - Reemplazar SimpleDateFormat/Calendar/Date con `kotlinx-datetime`:
//         `implementation("org.jetbrains.kotlinx:kotlinx-datetime:x.y.z")`
//         Es la librería oficial de JetBrains para fechas/horas en KMP.
//
// TODO: El polling de 100ms en AudioMessageBubble (LaunchedEffect) consume recursos
//       innecesariamente. Reemplazar con un callback `onCompletion` en AudioPlayer
//       que notifique cuando el audio termina, y un StateFlow para la posición.
//
// TODO: La lógica de resolución de ruta del audio (local → backend URL) está
//       duplicada en `AudioMessageBubble` y parcialmente en `AudioPlayer.android.kt`.
//       Centralizar esta lógica en el ViewModel o en el AudioPlayer.
//
// TODO: Agregar soporte para mostrar la duración total del audio (no solo el
//       tiempo transcurrido). Esto requiere cargar el archivo brevemente para
//       obtener su duración, o guardar la duración en MessageEntity al grabarlo.
//
// TODO: El indicador de "FAILED" (botón ↻) usa un carácter Unicode "↻" como icono.
//       Reemplazar con un ícono vectorial de Material Icons para consistencia visual
//       y mejor renderizado en todos los dispositivos.
//
// TODO: `LocalConfiguration.current` para el ancho de pantalla no está disponible
//       en todas las plataformas KMP. Para iOS, usar `LocalWindowInfo` o una
//       implementación expect/actual del ancho de pantalla.
