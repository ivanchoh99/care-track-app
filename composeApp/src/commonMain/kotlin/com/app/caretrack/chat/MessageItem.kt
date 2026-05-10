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
@Composable
fun MessageItem(
    message: ChatMessage,
    player: AudioPlayer? = null,
    onDelete: ((String) -> Unit)? = null,
    onRetry: ((String) -> Unit)? = null
) {
    val isMine = message.isMine

    // Calcular ancho máximo de burbuja (70% del ancho de pantalla)
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val maxBubbleWidth = (screenWidthDp * 0.7).dp

    val containerColor =
        if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val contentColor =
        if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDelete?.invoke(message.id)
            dismissState.reset()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {},
        enableDismissFromStartToEnd = false,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
        ) {
            Surface(
                modifier = Modifier.widthIn(max = maxBubbleWidth),
                color = containerColor,
                shape = if (isMine)
                    RoundedCornerShape(16.dp, 16.dp, 2.dp, 16.dp)
                else
                    RoundedCornerShape(16.dp, 16.dp, 16.dp, 2.dp),
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    when (message.type) {
                        MessageType.TEXT -> {
                            Text(text = message.content, color = contentColor)
                        }

                        MessageType.AUDIO -> {
                            if (player != null) {
                                AudioMessageBubble(message, player)
                            }
                        }

                        MessageType.DOCUMENT -> {
                            // Intentar cargar miniatura si existe el archivo local
                            val pdfFile = message.filePath?.let { path -> 
                                if (File(path).exists()) File(path) else null 
                            }
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = containerColor.copy(alpha = 0.3f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Icono grande de PDF o miniatura si está disponible
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
                                        modifier = Modifier
                                            .padding(start = 12.dp)
                                            .weight(1f)
                                    ) {
                                        Text(
                                            text = message.content,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = contentColor,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        // Mostrar tamaño si está disponible
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
                            val imagePath = message.filePath ?: message.backendUrl
                            if (imagePath != null) {
                                AsyncImage(
                                    model = imagePath,
                                    contentDescription = message.content,
                                    modifier = Modifier
                                        .size(200.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(message.content, color = contentColor)
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTimestamp(message.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor.copy(alpha = 0.6f),
                            fontSize = 10.sp
                        )

                        StatusIndicator(message = message, onRetry = onRetry)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusIndicator(message: ChatMessage, onRetry: ((String) -> Unit)?) {
    when (message.status) {
        MessageStatus.SENDING -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
            IconButton(
                onClick = { onRetry?.invoke(message.id) },
                modifier = Modifier.size(20.dp)
            ) {
                Text(
                    "↻",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }

        MessageStatus.SENT -> {}
        MessageStatus.PENDING -> {}
    }
}

private fun formatTimestamp(millis: Long): String {
    if (millis == 0L) return ""
    
    val now = Calendar.getInstance()
    val messageTime = Calendar.getInstance().apply { timeInMillis = millis }
    
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
    
    // Si es hoy, mostrar solo la hora
    if (messageTime.after(today)) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        return timeFormat.format(Date(millis))
    }
    
    // Si es ayer, mostrar "Ayer"
    if (messageTime.after(yesterday)) {
        return "Ayer"
    }
    
    // Si es de esta semana, mostrar el día
    val daysDiff = ((today.timeInMillis - messageTime.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
    if (daysDiff < 7) {
        val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault()) // Día de la semana
        return dayFormat.format(Date(millis))
    }
    
    // De lo contrario, mostrar fecha
    val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
    return dateFormat.format(Date(millis))
}

@Composable
fun AudioMessageBubble(message: ChatMessage, player: AudioPlayer) {
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var seconds by remember { mutableStateOf(0) }

    val dynamicTintColor =
        if (message.isMine) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

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
                // Verificar si terminó la reproducción
                val duration = player.getDuration()
                if (duration > 0 && progress >= 0.99f) {
                    // La reproducción terminó naturalmente
                    isPlaying = false
                    progress = 0f
                    seconds = 0
                } else if (!player.isPlaying() && progress > 0f) {
                    // Se detuvo por alguna razón
                    isPlaying = false
                    progress = 0f
                    seconds = 0
                }
            }
            delay(100) // Actualizar UI cada 100ms para evitar bloqueos
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = {
                AppLogger.d("AudioBubble", "Botón presionado - isPlaying: $isPlaying")
                
                if (isPlaying) {
                    AppLogger.d("AudioBubble", "Deteniendo reproducción...")
                    player.stopAudio()
                    isPlaying = false
                    progress = 0f
                    seconds = 0
                } else {
                    // Priorizar archivo local sobre URL del backend
                    val path = message.filePath
                    AppLogger.d("AudioBubble", "message.filePath: $path")
                    AppLogger.d("AudioBubble", "message.backendUrl: ${message.backendUrl}")
                    
                    if (!path.isNullOrBlank()) {
                        val audioFile = File(path)
                        AppLogger.d("AudioBubble", "Verificando archivo local - existe: ${audioFile.exists()}, tamaño: ${audioFile.length()} bytes")
                        
                        if (audioFile.exists()) {
                            player.playAudio(path)
                            isPlaying = true
                            AppLogger.d("AudioBubble", "Reproduciendo desde archivo local: $path")
                        } else {
                            AppLogger.w("AudioBubble", "Archivo local NO existe: $path")
                            // Fallback a URL del backend si archivo local no existe
                            val fallbackPath = message.backendUrl
                            if (!fallbackPath.isNullOrBlank()) {
                                AppLogger.d("AudioBubble", "Intentando con URL del backend: $fallbackPath")
                                player.playAudio(fallbackPath)
                                isPlaying = true
                            } else {
                                AppLogger.e("AudioBubble", "No hay URL de backend disponible")
                            }
                        }
                    } else {
                        // No hay path local, intentar con backendUrl
                        AppLogger.d("AudioBubble", "No hay filePath, intentando con backendUrl")
                        val fallbackPath = message.backendUrl
                        if (!fallbackPath.isNullOrBlank()) {
                            player.playAudio(fallbackPath)
                            isPlaying = true
                            AppLogger.d("AudioBubble", "Reproduciendo desde URL remota: $fallbackPath")
                        } else {
                            AppLogger.e("AudioBubble", "No se encontró archivo de audio - filePath y backendUrl son nulos")
                        }
                    }
                }
            }
        ) {
            Icon(
                painter = painterResource(
                    if (isPlaying) Res.drawable.stop_circle_24px
                    else Res.drawable.play_circle_24px
                ),
                contentDescription = if (isPlaying) stringResource(Res.string.action_stop) else stringResource(Res.string.action_play),
                tint = dynamicTintColor,
                modifier = Modifier.size(32.dp)
            )
        }

        Column(modifier = Modifier.width(150.dp).padding(start = 8.dp)) {
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
                val displayName = message.content.substringAfterLast("/").take(12)
                Text(
                    text = if (message.content.length > 12) "$displayName..." else displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = dynamicTintColor.copy(alpha = 0.8f)
                )

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
