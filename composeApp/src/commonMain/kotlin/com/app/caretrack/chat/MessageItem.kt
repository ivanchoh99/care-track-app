package com.app.caretrack.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
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

@Composable
fun MessageItem(
    message: ChatMessage,
    player: AudioPlayer? = null,
    onDelete: ((String) -> Unit)? = null,
    onRetry: ((String) -> Unit)? = null
) {
    val isMine = message.isMine

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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
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
    val totalSec = millis / 1000
    val minutes = (totalSec / 60) % 60
    val hours = totalSec / 3600
    return if (hours > 0) "%d:%02d".format(hours, minutes)
    else "%d min".format(minutes)
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
                    player.stopAudio()
                    isPlaying = false
                } else {
                    val path = message.filePath ?: message.backendUrl
                    if (!path.isNullOrBlank()) {
                        player.playAudio(path)
                        isPlaying = true
                    } else {
                        AppLogger.e("AudioBubble", "No se encontró archivo de audio")
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
