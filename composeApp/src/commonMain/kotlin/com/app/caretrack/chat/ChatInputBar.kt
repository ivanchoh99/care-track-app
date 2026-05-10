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

@Composable
fun ChatInputBar(
    isRecording: Boolean = false,
    onSendMessage: (String) -> Unit,
    onAttachmentSelected: (MessageType) -> Unit,
    onVoiceNoteStart: () -> Unit,
    onVoiceNoteEnd: () -> Unit
) {
    var textState by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp) // Margen externo para que flote
    ) {
        if (showMenu) {
            AttachmentMenu(
                onSelect = { type ->
                    onAttachmentSelected(type)
                    showMenu = false
                }
            )
        }

        // Usamos Surface para dar el aspecto redondeado y la elevación (sombra)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp), // Esquinas muy redondeadas como en la imagen
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically // Centrado vertical para que se vea prolijo
            ) {
                // Botón Adjuntar (+)
                IconButton(onClick = { showMenu = !showMenu }) {
                    Icon(
                        painter = painterResource(if (showMenu) Res.drawable.close_24px else Res.drawable.add_24px),
                        contentDescription = stringResource(Res.string.action_attach),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Campo de Texto sin indicadores
                TextField(
                    value = textState,
                    onValueChange = { textState = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(Res.string.chat_input_placeholder)) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    maxLines = 4
                )

                // Botón Dinámico (Enviar o Mic)
                if (textState.isNotBlank()) {
                    IconButton(
                        onClick = {
                            onSendMessage(textState)
                            textState = ""
                        }
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.send_24px),
                            contentDescription = stringResource(Res.string.action_send),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
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
                            painter = painterResource(if (isRecording) Res.drawable.stop_circle_24px else Res.drawable.mic_24px),
                            contentDescription = stringResource(Res.string.action_record),
                            tint = if (isRecording) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AttachmentMenu(onSelect: (MessageType) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AttachmentItem(Res.drawable.image_24px, stringResource(Res.string.attachment_image), Color(0xFF4CAF50)) {
                onSelect(
                    MessageType.IMAGE
                )
            }
            AttachmentItem(
                Res.drawable.picture_as_pdf_24px,
                stringResource(Res.string.attachment_pdf),
                Color(0xFFF44336)
            ) { onSelect(MessageType.DOCUMENT) }
            AttachmentItem(
                Res.drawable.audio_file_24px,
                stringResource(Res.string.attachment_audio),
                Color(0xFF2196F3)
            ) { onSelect(MessageType.AUDIO) }
        }
    }
}

@Composable
fun AttachmentItem(
    iconRes: org.jetbrains.compose.resources.DrawableResource,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.background(color.copy(alpha = 0.1f), CircleShape)
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}