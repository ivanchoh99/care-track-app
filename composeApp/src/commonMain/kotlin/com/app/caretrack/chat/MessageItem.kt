package com.app.caretrack.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import caretrack.composeapp.generated.resources.Res
import caretrack.composeapp.generated.resources.picture_as_pdf_24px
import caretrack.composeapp.generated.resources.play_circle_24px
import caretrack.composeapp.generated.resources.stop_circle_24px
import org.jetbrains.compose.resources.painterResource

@Composable
fun MessageItem(message: ChatMessage, player: AudioPlayer? = null) {
    val isMine = message.isMine
    val alignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart

    val containerColor =
        if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val contentColor =
        if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Surface(
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painterResource(Res.drawable.picture_as_pdf_24px),
                                null,
                                tint = contentColor
                            )
                            Text(
                                message.content,
                                color = contentColor,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }

                    MessageType.IMAGE -> {
                        Text("📷 ${message.content}", color = contentColor)
                    }
                }
            }
        }
    }
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
                isPlaying = false
                progress = 0f
                seconds = 0
            }
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = {
                if (isPlaying) {
                    // Si está reproduciendo, lo detenemos
                    player.stopAudio()
                    isPlaying = false
                } else {
                    // AQUÍ ESTÁ EL CAMBIO CLAVE: Usamos la ruta física guardada en Room
                    message.filePath?.let { path ->
                        player.playAudio(path)
                        isPlaying = true
                    } ?: run {
                        // Opcional: Mostrar un Toast o log si el archivo fue borrado del celular
                        println("No se encontró el archivo en la ruta: ${message.filePath}")
                    }
                }
            }
        ) {
            Icon(
                painter = painterResource(
                    if (isPlaying) Res.drawable.stop_circle_24px
                    else Res.drawable.play_circle_24px
                ),
                contentDescription = if (isPlaying) "Detener" else "Reproducir",
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
                // Formateamos el nombre del archivo para que no rompa el diseño
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